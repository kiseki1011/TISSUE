package com.tissue.global.exception.base;

import com.tissue.global.exception.ErrorCode;
import com.tissue.global.exception.TissueException;
import org.springframework.http.HttpStatus;

public class ExternalServiceException extends TissueException {

    @Override
    public final HttpStatus getHttpStatus() {
        return HttpStatus.SERVICE_UNAVAILABLE;
    }

    public ExternalServiceException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ExternalServiceException(ErrorCode errorCode, String debugMessage) {
        super(errorCode, debugMessage);
    }

    public ExternalServiceException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
