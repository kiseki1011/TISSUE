package com.tissue.domain.exception;

import com.tissue.shared.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthenticationErrorCode implements ErrorCode {
    EMAIL_NOT_VERIFIED("Email is not verified"),
    MEMBER_SIGNUP_CONFLICT("Member signup failed due to duplicate email or username"),
    OWNER_NOT_WITHDRAWABLE("Cannot withdraw if you're a workspace owner"),
    EMAIL_SIGNUP_DISABLED("Email signup is currently disabled by policy"),
    UNAUTHORIZED_DOMAIN("Email domain is not authorized by policy"),
    EMAIL_AUTHENTICATION_IDENTITY_NOT_FOUND("Could not find the email authentication identity"),
    VERIFICATION_TOKEN_DUPLICATE("A verification email was already sent recently");

    private final String defaultMessage;
}
