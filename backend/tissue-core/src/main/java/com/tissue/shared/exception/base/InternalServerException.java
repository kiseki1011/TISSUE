package com.tissue.shared.exception.base;

import com.tissue.shared.exception.ErrorCode;
import com.tissue.shared.exception.TissueException;
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
