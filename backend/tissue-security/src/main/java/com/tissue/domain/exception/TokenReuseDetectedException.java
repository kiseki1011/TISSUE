package com.tissue.domain.exception;

import com.tissue.shared.exception.base.UnauthorizedException;

public class TokenReuseDetectedException extends UnauthorizedException {

    public TokenReuseDetectedException() {
        super(AuthenticationErrorCode.TOKEN_REUSE_DETECTED);
    }
}
