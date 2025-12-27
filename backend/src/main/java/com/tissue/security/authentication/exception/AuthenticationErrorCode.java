package com.tissue.security.authentication.exception;

import com.tissue.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthenticationErrorCode implements ErrorCode {
    INVALID_TOKEN("Invalid token"),
    EXPIRED_TOKEN("Token is expired"),
    UNSUPPORTED_TOKEN("Unsupported token"),
    MALFORMED_TOKEN("Malformed token"),
    TOKEN_SIGNATURE_INVALID("Invalid token signature"),
    TOKEN_TYPE_MISMATCH("Token type mismatch"),
    TOKEN_MISSING_CLAIM("Token is missing required claim"),
    LOGIN_FAILED("Login failed"),
    INVALID_VERIFICATION_TOKEN("Invalid verification token"),
    VERIFICATION_TOKEN_EXPIRED("Verification token is expired"),
    EMAIL_NOT_VERIFIED("Email is not verified"),
    ELEVATED_PERMISSION_REQUIRED("Elevated permission required for this operation");

    private final String defaultMessage;
}
