package com.tissue.vcs.domain.exception;

import com.tissue.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum VcsErrorCode implements ErrorCode {
    INTEGRATION_NOT_FOUND("VCS integration not found for workspace"),
    INVALID_WEBHOOK_SECRET("Invalid webhook secret"),
    WEBHOOK_PROCESSING_ERROR("Failed to process webhook"),
    INVALID_WEBHOOK_PAYLOAD("Invalid webhook payload"),
    MISSING_SIGNATURE("Missing signature header");

    private final String defaultMessage;
}
