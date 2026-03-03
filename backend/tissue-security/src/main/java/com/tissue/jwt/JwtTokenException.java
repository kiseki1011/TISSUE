package com.tissue.jwt;

import com.tissue.domain.exception.AuthenticationErrorCode;
import com.tissue.shared.exception.base.UnauthorizedException;

public class JwtTokenException extends UnauthorizedException {
    public JwtTokenException() {
        super(AuthenticationErrorCode.INVALID_TOKEN);
    }

    public JwtTokenException(String msg, Throwable cause) {
        super(AuthenticationErrorCode.INVALID_TOKEN, cause);
    }
}
