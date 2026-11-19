package com.artbazaar.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Getter
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class UnauthorizedException extends RuntimeException {

    /** Optional machine-readable reason the client can act on, e.g. {@code REFRESH_RACE}. */
    private final String code;

    public UnauthorizedException(String message) {
        this(message, null);
    }

    public UnauthorizedException(String message, String code) {
        super(message);
        this.code = code;
    }
}
