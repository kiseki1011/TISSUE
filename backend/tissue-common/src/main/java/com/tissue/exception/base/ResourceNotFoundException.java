package com.tissue.exception.base;

import com.tissue.exception.ErrorCode;
import com.tissue.exception.TissueException;
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
