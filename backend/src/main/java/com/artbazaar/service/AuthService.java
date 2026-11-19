package com.artbazaar.service;

import com.artbazaar.dto.auth.AuthResponse;
import com.artbazaar.dto.auth.AuthResult;
import com.artbazaar.dto.auth.LoginRequest;
import com.artbazaar.dto.auth.RegisterRequest;
import com.artbazaar.dto.auth.UserResponse;
import com.artbazaar.entity.ArtistProfile;
import com.artbazaar.entity.Role;
import com.artbazaar.entity.User;
import com.artbazaar.exception.ConflictException;
import com.artbazaar.exception.UnauthorizedException;
import com.artbazaar.repository.ArtistProfileRepository;
import com.artbazaar.repository.UserRepository;
import com.artbazaar.security.CurrentUserLoader;
import com.artbazaar.security.JwtService;
import com.artbazaar.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String INVALID_LOGIN = "Invalid email or password";

    private final UserRepository userRepository;
    private final ArtistProfileRepository artistProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final CurrentUserLoader currentUserLoader;

    /** Hash checked when the email is unknown, so response time does not reveal which emails exist. */
    private String dummyHash;

    @Transactional
    public AuthResult register(RegisterRequest req) {
        String email = normalizeEmail(req.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("An account with this email already exists");
        }

        User user = new User();
        user.setName(req.name().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRole(req.accountType() == RegisterRequest.AccountType.ARTIST ? Role.ARTIST : Role.BUYER);
        userRepository.save(user);

        if (user.getRole() == Role.ARTIST) {
            ArtistProfile profile = new ArtistProfile();
            profile.setUser(user);
            profile.setBio(trimToNull(req.bio()));
            profile.setCity(trimToNull(req.city()));
            artistProfileRepository.save(profile); // approved = false until an admin approves
        }
        return issueTokens(user);
    }

    @Transactional
    public AuthResult login(LoginRequest req) {
        User user = userRepository.findByEmail(normalizeEmail(req.email())).orElse(null);
        if (user == null) {
            passwordEncoder.matches(req.password(), dummyHash());
            throw new UnauthorizedException(INVALID_LOGIN);
        }
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new UnauthorizedException(INVALID_LOGIN);
        }
        if (!user.isEnabled()) {
            throw new UnauthorizedException("This account has been disabled. Contact support for help.");
        }
        return issueTokens(user);
    }

    /** noRollbackFor: when token reuse is detected, the "revoke every session" write must still commit. */
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public AuthResult refresh(String refreshToken) {
        User user = refreshTokenService.consume(refreshToken);
        return issueTokens(user);
    }

    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    private AuthResult issueTokens(User user) {
        UserPrincipal principal = UserPrincipal.of(user, currentUserLoader.isApprovedArtist(user));
        AuthResponse body = new AuthResponse(jwtService.createAccessToken(user), jwtService.accessTokenTtlSeconds(),
                UserResponse.from(principal));
        return new AuthResult(body, refreshTokenService.issue(user));
    }

    private String dummyHash() {
        if (dummyHash == null) {
            dummyHash = passwordEncoder.encode("not-a-real-password-1");
        }
        return dummyHash;
    }

    static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String trimToNull(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }
}
