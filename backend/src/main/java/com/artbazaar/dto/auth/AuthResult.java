package com.artbazaar.dto.auth;

/** What the auth service hands the controller: the response body plus the raw refresh token for the cookie. */
public record AuthResult(AuthResponse body, String refreshToken) {
}
