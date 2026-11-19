package com.artbazaar.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Getter
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class UnauthorizedException extends RuntimeException {

    /** Optional machine-readable reason a client can act on (included in the error body as "code"). */
    private final String code;

    public UnauthorizedException(String message) {
        this(message, null);
    }

    public UnauthorizedException(String message, String code) {
        super(message);
        this.code = code;
    }
}
