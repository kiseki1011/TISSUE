package com.tissue.common.exception.base;

import com.tissue.common.exception.ErrorCode;
import com.tissue.common.exception.TissueException;
import org.springframework.http.HttpStatus;

public class BadRequestException extends TissueException {

    @Override
    public final HttpStatus getHttpStatus() {
        return HttpStatus.BAD_REQUEST;
    }

    public BadRequestException(ErrorCode errorCode) {
        super(errorCode);
    }

    public BadRequestException(ErrorCode errorCode, String loggingMessage) {
        super(errorCode, loggingMessage);
    }
}
