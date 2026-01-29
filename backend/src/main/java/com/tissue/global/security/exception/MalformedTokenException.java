package com.tissue.global.security.exception;

import org.springframework.security.core.AuthenticationException;

public class MalformedTokenException extends AuthenticationException {

    public MalformedTokenException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public MalformedTokenException(String msg) {
        super(msg);
    }
}
