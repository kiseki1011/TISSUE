package com.tissue.shared.exception.base;

import com.tissue.shared.exception.ErrorCode;
import com.tissue.shared.exception.TissueException;
import org.springframework.http.HttpStatus;

public abstract class UnauthorizedException extends TissueException {

    @Override
    public final HttpStatus getHttpStatus() {
        return HttpStatus.UNAUTHORIZED;
    }

    protected UnauthorizedException(ErrorCode errorCode) {
        super(errorCode);
    }

    protected UnauthorizedException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
