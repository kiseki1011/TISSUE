package com.tissue.feature.vcs.domain.exception;

import com.tissue.shared.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum VcsErrorCode implements ErrorCode {
    INTEGRATION_NOT_FOUND(HttpStatus.NOT_FOUND, "VCS integration not found for project"),
    INVALID_WEBHOOK_SECRET(HttpStatus.FORBIDDEN, "Invalid webhook secret"),
    MISSING_SIGNATURE(HttpStatus.FORBIDDEN, "Missing signature header"),
    WEBHOOK_PROCESSING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to process webhook"),
    INVALID_WEBHOOK_PAYLOAD(HttpStatus.BAD_REQUEST, "Invalid webhook payload");

    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
