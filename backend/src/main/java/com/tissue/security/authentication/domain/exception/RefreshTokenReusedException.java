package com.tissue.security.authentication.domain.exception;

import org.springframework.security.core.AuthenticationException;

public class RefreshTokenReusedException extends AuthenticationException {

    public RefreshTokenReusedException(String msg) {
        super(msg);
    }
}
