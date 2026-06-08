package com.tissue.shared.exception;

import static com.tissue.shared.exception.ErrorContextKeys.CURSOR_TOKEN;

import com.tissue.shared.exception.base.BadRequestException;

public class InvalidCursorException extends BadRequestException {

    public InvalidCursorException(String token, Throwable cause) {
        super(CommonErrorCode.INVALID_CURSOR, cause);
        addContext(CURSOR_TOKEN, token);
    }
}
