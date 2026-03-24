package com.tissue.security.domain.exception;

import com.tissue.shared.exception.base.UnauthorizedException;

public class TokenExpiredException extends UnauthorizedException {

    public TokenExpiredException() {
        super(AuthenticationErrorCode.EXPIRED_TOKEN);
    }
}
