package com.tissue.global.security.jwt;

import com.tissue.global.security.exception.InvalidTokenException;

/**
 * JWT specific exception located in the infrastructure layer.
 */
public class JwtTokenException extends InvalidTokenException {
    public JwtTokenException(String msg) {
        super(msg);
    }

    public JwtTokenException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
