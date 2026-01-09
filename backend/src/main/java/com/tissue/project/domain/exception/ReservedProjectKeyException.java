package com.tissue.project.domain.exception;

import static com.tissue.global.exception.ContextKeys.PROJECT_KEY;

import com.tissue.global.exception.base.BadRequestException;

public class ReservedProjectKeyException extends BadRequestException {

    public ReservedProjectKeyException(String projectKey) {
        super(ProjectErrorCode.RESERVED_PROJECT_KEY);
        addContext(PROJECT_KEY, projectKey);
    }
}
