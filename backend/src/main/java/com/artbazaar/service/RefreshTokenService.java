package com.artbazaar.service;

import com.artbazaar.config.AppProperties;
import com.artbazaar.entity.RefreshToken;
import com.artbazaar.entity.User;
import com.artbazaar.exception.UnauthorizedException;
import com.artbazaar.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Long-lived refresh tokens with rotation: every refresh revokes the presented token and
 * issues a new one. If an already-revoked token is presented again, it was probably stolen,
 * so every session of that user is revoked. A short grace period tolerates the benign race of two
 * tabs refreshing with the same cookie at the same moment.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();
    static final Duration REUSE_GRACE = Duration.ofSeconds(10);
    public static final String REFRESH_RACE = "REFRESH_RACE";

    private final RefreshTokenRepository repository;
    private final AppProperties props;
    private final Clock clock;

    @Transactional
    public String issue(User user) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(hash(raw));
        token.setExpiresAt(clock.instant().plus(props.jwt().refreshTokenTtl()));
        repository.save(token);
        return raw;
    }

    /** Validates and revokes the presented token, returning its owner. Caller issues the replacement. */
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public User consume(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new UnauthorizedException("Session expired. Please sign in again.");
        }
        Instant now = clock.instant();
        RefreshToken token = repository.findByTokenHash(hash(raw))
                .orElseThrow(() -> new UnauthorizedException("Session expired. Please sign in again."));

        if (token.isRevoked()) {
            if (token.getRevokedAt().isAfter(now.minus(REUSE_GRACE))) {
                // Concurrent refresh from another tab: reject, the client retries with the new cookie.
                throw new UnauthorizedException("Session was just refreshed elsewhere", REFRESH_RACE);
            }
            log.warn("Reuse of revoked refresh token for user {}; revoking all sessions", token.getUser().getId());
            repository.revokeAllForUser(token.getUser().getId(), now);
            throw new UnauthorizedException("Session expired. Please sign in again.");
        }
        if (token.isExpired(now) || !token.getUser().isEnabled()) {
            throw new UnauthorizedException("Session expired. Please sign in again.");
        }
        token.setRevokedAt(now);
        return token.getUser();
    }

    @Transactional
    public void revoke(String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        repository.findByTokenHash(hash(raw))
                .filter(t -> !t.isRevoked())
                .ifPresent(t -> t.setRevokedAt(clock.instant()));
    }

    @Transactional
    public void revokeAll(Long userId) {
        repository.revokeAllForUser(userId, clock.instant());
    }

    public Duration ttl() {
        return props.jwt().refreshTokenTtl();
    }

    /** Daily housekeeping: drop tokens that expired more than a day ago. */
    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void purgeExpired() {
        int removed = repository.deleteExpiredBefore(clock.instant().minus(Duration.ofDays(1)));
        if (removed > 0) {
            log.info("Purged {} expired refresh tokens", removed);
        }
    }

    static String hash(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
