package com.tissue.security.authentication.domain.exception;

import org.springframework.security.core.AuthenticationException;

public class JwtSecretException extends AuthenticationException {

    public JwtSecretException(String message) {
        super(message);
    }

    public JwtSecretException(String message, Throwable cause) {
        super(message, cause);
    }
}
