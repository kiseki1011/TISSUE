package com.tissue.shared.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Too many requests. Please try again later");

    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
