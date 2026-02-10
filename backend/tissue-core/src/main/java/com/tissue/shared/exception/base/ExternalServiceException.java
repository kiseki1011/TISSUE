package com.tissue.shared.exception.base;

import com.tissue.shared.exception.ErrorCode;
import com.tissue.shared.exception.TissueException;
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
