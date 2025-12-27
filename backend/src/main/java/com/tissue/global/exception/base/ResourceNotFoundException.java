package com.tissue.global.exception.base;

import com.tissue.global.exception.ErrorCode;
import com.tissue.global.exception.TissueException;
import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends TissueException {

    @Override
    public final HttpStatus getHttpStatus() {
        return HttpStatus.NOT_FOUND;
    }

    public ResourceNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }
}
