package com.tissue.security.domain.exception;

import com.tissue.shared.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthenticationErrorCode implements ErrorCode {
    EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN, "Email is not verified"),
    MEMBER_SIGNUP_CONFLICT(HttpStatus.CONFLICT, "Member signup failed due to duplicate email or username"),
    OWNER_NOT_WITHDRAWABLE(HttpStatus.BAD_REQUEST, "Cannot withdraw if you're a workspace owner"),
    EMAIL_SIGNUP_DISABLED(HttpStatus.FORBIDDEN, "Email signup is currently disabled by policy"),
    SIGNUP_BLOCKED_NO_WORKSPACE(
            HttpStatus.FORBIDDEN,
            "Signup is temporarily unavailable. The administrator must create a workspace first."),
    EMAIL_AUTHENTICATION_IDENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "Could not find the email authentication identity"),
    VERIFICATION_TOKEN_DUPLICATE(HttpStatus.CONFLICT, "A verification email was already sent recently"),
    EMAIL_IDENTITY_ALREADY_EXISTS(HttpStatus.CONFLICT, "Email authentication identity already exists"),
    OAUTH_IDENTITY_ALREADY_LINKED(HttpStatus.CONFLICT, "OAuth account is already linked to a member"),

    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "The provided token is invalid"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "The provided token has expired"),
    // Unused: best-guess BAD_REQUEST (password reset flow validation)
    INVALID_PASSWORD_RESET_CODE(HttpStatus.BAD_REQUEST, "The password reset code is invalid or has expired"),
    INVALID_PASSWORD_RESET_TOKEN(HttpStatus.BAD_REQUEST, "The password reset token is invalid or has expired"),
    TOKEN_REUSE_DETECTED(HttpStatus.UNAUTHORIZED, "Token reuse detected"),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "Active session not found or has expired"),

    LOGIN_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Too many login attempts. Please try again later"),
    // Unused: best-guess TOO_MANY_REQUESTS (rate-limit on email feature)
    EMAIL_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Too many requests. Please try again later"),
    EMAIL_FEATURE_DISABLED(HttpStatus.BAD_REQUEST, "Email feature is disabled on this server");

    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
