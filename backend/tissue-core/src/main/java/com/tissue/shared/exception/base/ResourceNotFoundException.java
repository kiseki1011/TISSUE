package com.tissue.shared.exception.base;

import com.tissue.shared.exception.ErrorCode;
import com.tissue.shared.exception.TissueException;
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
