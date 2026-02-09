package com.tissue.exception.base;

import com.tissue.exception.ErrorCode;
import com.tissue.exception.TissueException;
import org.springframework.http.HttpStatus;

public class ForbiddenException extends TissueException {

    @Override
    public final HttpStatus getHttpStatus() {
        return HttpStatus.FORBIDDEN;
    }

    public ForbiddenException(ErrorCode errorCode) {
        super(errorCode);
    }
}
