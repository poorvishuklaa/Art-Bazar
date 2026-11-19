package com.artbazaar.dto.auth;

/** Body of login/register/refresh. The refresh token travels separately, in an HttpOnly cookie. */
public record AuthResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {

    public AuthResponse(String accessToken, long expiresIn, UserResponse user) {
        this(accessToken, "Bearer", expiresIn, user);
    }
}
