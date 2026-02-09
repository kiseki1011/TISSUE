package com.tissue.global.security.exception;

import org.springframework.security.core.AuthenticationException;

/**
 * Common exception for all JWT related authentication failures.
 */
public class JwtTokenException extends AuthenticationException {
    public JwtTokenException(String msg) {
        super(msg);
    }

    public JwtTokenException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
