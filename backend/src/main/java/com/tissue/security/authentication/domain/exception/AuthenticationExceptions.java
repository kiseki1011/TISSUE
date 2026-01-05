package com.tissue.security.authentication.domain.exception;

import com.tissue.global.exception.base.BadRequestException;
import com.tissue.global.exception.base.ForbiddenException;

public class AuthenticationExceptions {

    private AuthenticationExceptions() {}

    // TODO: if this is a ForbiddenException should i just move this to MemberExceptions?
    public static ForbiddenException elevatedPermissionRequired() {
        return new ForbiddenException(AuthenticationErrorCode.ELEVATED_PERMISSION_REQUIRED);
    }

    // TODO: if this is a BadRequestException should i just move this to MemberExceptions?
    public static BadRequestException invalidVerificationToken() {
        return new BadRequestException(AuthenticationErrorCode.INVALID_VERIFICATION_TOKEN);
    }

    public static BadRequestException verificationTokenExpired() {
        return new BadRequestException(AuthenticationErrorCode.VERIFICATION_TOKEN_EXPIRED);
    }
}
