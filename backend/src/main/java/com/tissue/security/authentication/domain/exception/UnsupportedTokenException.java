package com.tissue.security.authentication.domain.exception;

import org.springframework.security.core.AuthenticationException;

public class UnsupportedTokenException extends AuthenticationException {

    public UnsupportedTokenException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public UnsupportedTokenException(String msg) {
        super(msg);
    }
}
