package com.tissue.vcs.domain.exception;

import com.tissue.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum VcsErrorCode implements ErrorCode {

    INTEGRATION_NOT_FOUND("VCS integration not found for workspace"),
    INVALID_WEBHOOK_SECRET("Invalid webhook secret");

    private final String defaultMessage;
}
