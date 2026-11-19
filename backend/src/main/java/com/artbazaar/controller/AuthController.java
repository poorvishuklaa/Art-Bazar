package com.artbazaar.controller;

import com.artbazaar.config.AppProperties;
import com.artbazaar.dto.auth.AuthResponse;
import com.artbazaar.dto.auth.AuthResult;
import com.artbazaar.dto.auth.LoginRequest;
import com.artbazaar.dto.auth.RegisterRequest;
import com.artbazaar.dto.auth.UserResponse;
import com.artbazaar.security.UserPrincipal;
import com.artbazaar.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * Access token in the JSON body (kept in memory by the client); refresh token in an
 * HttpOnly, SameSite=Strict cookie scoped to /api/auth, so page scripts can never read it.
 */
@Tag(name = "Auth")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    static final String REFRESH_COOKIE = "ab_refresh";
    private static final String COOKIE_PATH = "/api/auth";

    private final AuthService authService;
    private final AppProperties props;

    @Operation(summary = "Create a buyer or artist account")
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return withCookie(HttpStatus.CREATED, authService.register(request));
    }

    @Operation(summary = "Sign in with email and password")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return withCookie(HttpStatus.OK, authService.login(request));
    }

    @Operation(summary = "Exchange the refresh cookie for a new access token (rotates the cookie)")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@CookieValue(name = REFRESH_COOKIE, required = false) String token) {
        return withCookie(HttpStatus.OK, authService.refresh(token));
    }

    @Operation(summary = "Sign out: revokes the refresh token and clears the cookie")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = REFRESH_COOKIE, required = false) String token) {
        authService.logout(token);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString())
                .build();
    }

    @Operation(summary = "The signed-in user")
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        return UserResponse.from(principal);
    }

    private ResponseEntity<AuthResponse> withCookie(HttpStatus status, AuthResult result) {
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, cookie(result.refreshToken(), props.jwt().refreshTokenTtl()).toString())
                .body(result.body());
    }

    private ResponseCookie cookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(props.jwt().refreshCookieSecure())
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(maxAge)
                .build();
    }
}
