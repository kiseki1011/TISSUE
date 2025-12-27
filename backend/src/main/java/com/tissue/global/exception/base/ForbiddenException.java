package com.tissue.global.exception.base;

import com.tissue.global.exception.ErrorCode;
import com.tissue.global.exception.TissueException;
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
