package com.tissue.global.security.exception;

import org.springframework.security.core.AuthenticationException;

/**
 * Thrown when an authentication token is missing or invalid.
 */
public class InvalidTokenException extends AuthenticationException {
    public InvalidTokenException(String msg) {
        super(msg);
    }

    public InvalidTokenException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
