package com.tissue.common.exception.base;

import com.tissue.common.exception.ErrorCode;
import com.tissue.common.exception.TissueException;
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
