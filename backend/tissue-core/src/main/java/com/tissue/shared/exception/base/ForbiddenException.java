package com.tissue.shared.exception.base;

import com.tissue.shared.exception.ErrorCode;
import com.tissue.shared.exception.TissueException;
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
