package com.tissue.global.security.exception;

import com.tissue.common.exception.base.InternalServerException;

public class JwtCreationException extends InternalServerException {

    public JwtCreationException(String message) {
        super(AuthenticationErrorCode.JWT_CREATION_ERROR, message);
    }

    public JwtCreationException(String message, Throwable cause) {
        super(AuthenticationErrorCode.JWT_CREATION_ERROR, cause);
    }
}
