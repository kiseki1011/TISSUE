package com.tissue.feature.issue.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.CURSOR_TOKEN;

import com.tissue.shared.exception.base.BadRequestException;

public class InvalidCursorException extends BadRequestException {

    public InvalidCursorException(String token, Throwable cause) {
        super(IssueErrorCode.INVALID_CURSOR_TOKEN, cause);
        addContext(CURSOR_TOKEN, token);
    }
}
