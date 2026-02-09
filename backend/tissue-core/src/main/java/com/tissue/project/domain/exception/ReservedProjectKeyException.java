package com.tissue.project.domain.exception;

import static com.tissue.exception.ErrorContextKeys.PROJECT_KEY;

import com.tissue.exception.base.BadRequestException;

public class ReservedProjectKeyException extends BadRequestException {

    public ReservedProjectKeyException(String projectKey) {
        super(ProjectErrorCode.RESERVED_PROJECT_KEY);
        addContext(PROJECT_KEY, projectKey);
    }
}
