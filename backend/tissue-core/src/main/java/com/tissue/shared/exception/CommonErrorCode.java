package com.tissue.shared.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {
    RATE_LIMITED("Too many requests. Please try again later");

    private final String defaultMessage;
}
