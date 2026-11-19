package com.artbazaar.security;

import com.artbazaar.config.AppProperties;
import com.artbazaar.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/** Issues and verifies short-lived HS256 access tokens. */
@Slf4j
@Service
public class JwtService {

    private static final String ISSUER = "art-bazaar";
    private static final String DEV_SECRET_PREFIX = "ZGV2LW9ubHkt"; // base64 of "dev-only-"

    private final SecretKey key;
    private final Duration accessTokenTtl;
    private final Clock clock;

    public JwtService(AppProperties props, Clock clock) {
        byte[] keyBytes = Decoders.BASE64.decode(props.jwt().secret());
        if (keyBytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret must be at least 256 bits (32 bytes) once base64-decoded");
        }
        if (props.jwt().secret().startsWith(DEV_SECRET_PREFIX)) {
            log.warn("Using the development JWT secret. Set JWT_SECRET before deploying.");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenTtl = props.jwt().accessTokenTtl();
        this.clock = clock;
    }

    public String createAccessToken(User user) {
        Instant now = clock.instant();
        return Jwts.builder()
                .issuer(ISSUER)
                .subject(String.valueOf(user.getId()))
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenTtl)))
                .signWith(key)
                .compact();
    }

    /**
     * @return the user id in the token
     * @throws JwtException if the token is malformed, expired or not signed by us
     */
    public Long parseUserId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(ISSUER)
                .clock(() -> Date.from(clock.instant()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Long.valueOf(claims.getSubject());
    }

    public long accessTokenTtlSeconds() {
        return accessTokenTtl.toSeconds();
    }
}
