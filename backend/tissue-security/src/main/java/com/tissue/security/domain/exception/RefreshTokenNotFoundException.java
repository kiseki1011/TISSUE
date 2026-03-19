package com.tissue.security.domain.exception;

import com.tissue.shared.exception.base.UnauthorizedException;

public class RefreshTokenNotFoundException extends UnauthorizedException {

    public RefreshTokenNotFoundException() {
        super(AuthenticationErrorCode.REFRESH_TOKEN_NOT_FOUND);
    }
}
