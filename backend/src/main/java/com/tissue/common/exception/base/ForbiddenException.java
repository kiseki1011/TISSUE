package com.tissue.common.exception.base;

import com.tissue.common.exception.ErrorCode;
import com.tissue.common.exception.TissueException;
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
