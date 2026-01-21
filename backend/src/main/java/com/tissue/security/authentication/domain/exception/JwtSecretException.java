package com.tissue.security.authentication.domain.exception;

import com.tissue.global.exception.base.InternalServerException;

public class JwtSecretException extends InternalServerException {

    public JwtSecretException(String message) {
        super(AuthenticationErrorCode.JWT_SECRET_ERROR, message);
    }

    public JwtSecretException(String message, Throwable cause) {
        super(AuthenticationErrorCode.JWT_SECRET_ERROR, cause);
    }
}
