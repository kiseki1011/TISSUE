package com.tissue.security.authentication.domain.exception;

import org.springframework.security.core.AuthenticationException;

public class TokenMissingClaimException extends AuthenticationException {

    public TokenMissingClaimException(String msg) {
        super(msg);
    }
}
