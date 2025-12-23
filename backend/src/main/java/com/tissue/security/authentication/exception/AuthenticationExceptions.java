package com.tissue.security.authentication.exception;

import static com.tissue.security.authentication.exception.AuthenticationErrorCode.*;

import com.tissue.global.exception.base.BadRequestException;
import com.tissue.global.exception.base.ForbiddenException;

public class AuthenticationExceptions {

	private AuthenticationExceptions() {
	}

	// TODO: if this is a ForbiddenException should i just move this to MemberExceptions?
	public static ForbiddenException elevatedPermissionRequired() {
		return new ForbiddenException(ELEVATED_PERMISSION_REQUIRED);
	}

	// TODO: if this is a BadRequestException should i just move this to MemberExceptions?
	public static BadRequestException invalidVerificationToken() {
		return new BadRequestException(INVALID_VERIFICATION_TOKEN);
	}

	public static BadRequestException verificationTokenExpired() {
		return new BadRequestException(VERIFICATION_TOKEN_EXPIRED);
	}
}
