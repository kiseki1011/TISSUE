package com.tissue.shared.exception.base;

import com.tissue.shared.exception.ErrorCode;
import com.tissue.shared.exception.TissueException;
import org.springframework.http.HttpStatus;

public class RateLimitExceededException extends TissueException {

    @Override
    public final HttpStatus getHttpStatus() {
        return HttpStatus.TOO_MANY_REQUESTS;
    }

    public RateLimitExceededException(ErrorCode errorCode) {
        super(errorCode);
    }

    public RateLimitExceededException(ErrorCode errorCode, String detailMessage) {
        super(errorCode, detailMessage);
    }
}
