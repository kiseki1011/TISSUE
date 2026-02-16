package com.tissue.jwt;

import com.tissue.domain.exception.InvalidTokenException;

public class JwtTokenException extends InvalidTokenException {
    public JwtTokenException(String msg) {
        super(msg);
    }

    public JwtTokenException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
