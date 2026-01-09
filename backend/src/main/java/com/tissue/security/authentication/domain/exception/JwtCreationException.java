package com.tissue.security.authentication.domain.exception;

import org.springframework.security.core.AuthenticationException;

public class JwtCreationException extends AuthenticationException {

    public JwtCreationException(String message) {
        super(message);
    }

    public JwtCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
