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
    EMAIL_SIGNUP_DISABLED(HttpStatus.FORBIDDEN, "Email signup is currently disabled by policy"),
    EMAIL_AUTHENTICATION_IDENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "Could not find the email authentication identity"),
    VERIFICATION_TOKEN_DUPLICATE(HttpStatus.CONFLICT, "A verification email was already sent recently"),
    EMAIL_IDENTITY_ALREADY_EXISTS(HttpStatus.CONFLICT, "Email authentication identity already exists"),

    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "The provided token is invalid"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "The provided token has expired"),

    INVALID_PASSWORD_RESET_TOKEN(HttpStatus.BAD_REQUEST, "The password reset token is invalid or has expired"),
    TOKEN_REUSE_DETECTED(HttpStatus.UNAUTHORIZED, "Token reuse detected"),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "Active session not found or has expired"),

    LOGIN_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Too many login attempts; please try again later"),

    EMAIL_FEATURE_DISABLED(HttpStatus.BAD_REQUEST, "Email feature is disabled on this server"),

    RESTORE_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid credentials for account restore"),
    RESTORE_NOT_DELETED(HttpStatus.CONFLICT, "Account is not DELETED status; cannot restore");

    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
