package com.tissue.exception.base;

import com.tissue.exception.ErrorCode;
import com.tissue.exception.TissueException;
import org.springframework.http.HttpStatus;

public class InternalServerException extends TissueException {

    @Override
    public final HttpStatus getHttpStatus() {
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    public InternalServerException(ErrorCode errorCode) {
        super(errorCode);
    }

    public InternalServerException(ErrorCode errorCode, String debugMessage) {
        super(errorCode, debugMessage);
    }

    public InternalServerException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
