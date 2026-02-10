package com.tissue.feature.project.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.PROJECT_KEY;

import com.tissue.shared.exception.base.BadRequestException;

public class ReservedProjectKeyException extends BadRequestException {

    public ReservedProjectKeyException(String projectKey) {
        super(ProjectErrorCode.RESERVED_PROJECT_KEY);
        addContext(PROJECT_KEY, projectKey);
    }
}
