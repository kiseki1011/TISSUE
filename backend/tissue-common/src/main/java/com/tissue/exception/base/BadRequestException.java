package com.tissue.exception.base;

import com.tissue.exception.ErrorCode;
import com.tissue.exception.TissueException;
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
