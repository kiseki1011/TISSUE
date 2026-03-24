package com.tissue.security.domain.exception;

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
    VERIFICATION_TOKEN_DUPLICATE("A verification email was already sent recently"),
    EMAIL_IDENTITY_ALREADY_EXISTS("Email authentication identity already exists"),
    OAUTH_IDENTITY_ALREADY_LINKED("OAuth account is already linked to a member"),

    INVALID_TOKEN("The provided token is invalid"),
    EXPIRED_TOKEN("The provided token has expired"),
    INVALID_PASSWORD_RESET_CODE("The password reset code is invalid or has expired"),
    INVALID_PASSWORD_RESET_TOKEN("The password reset token is invalid or has expired"),
    TOKEN_REUSE_DETECTED("Token reuse detected"),
    REFRESH_TOKEN_NOT_FOUND("Active session not found or has expired"),

    LOGIN_RATE_LIMITED("Too many login attempts. Please try again later"),
    EMAIL_RATE_LIMITED("Too many requests. Please try again later");

    private final String defaultMessage;
}
