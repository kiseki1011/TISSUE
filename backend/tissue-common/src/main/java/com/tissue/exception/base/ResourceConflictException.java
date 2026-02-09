package com.tissue.exception.base;

import com.tissue.exception.ErrorCode;
import com.tissue.exception.TissueException;
import org.springframework.http.HttpStatus;

public class ResourceConflictException extends TissueException {

    @Override
    public final HttpStatus getHttpStatus() {
        return HttpStatus.CONFLICT;
    }

    public ResourceConflictException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ResourceConflictException(ErrorCode errorCode, Throwable e) {
        super(errorCode, e);
    }

    public ResourceConflictException(ErrorCode errorCode, String debugMessage) {
        super(errorCode, debugMessage);
    }
}
