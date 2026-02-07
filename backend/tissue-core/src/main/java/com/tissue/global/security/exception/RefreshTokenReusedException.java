package com.tissue.global.security.exception;

import org.springframework.security.core.AuthenticationException;

public class RefreshTokenReusedException extends AuthenticationException {

    public RefreshTokenReusedException(String msg) {
        super(msg);
    }
}
