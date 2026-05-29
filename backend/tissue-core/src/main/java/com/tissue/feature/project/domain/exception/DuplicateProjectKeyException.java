package com.tissue.feature.project.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.PROJECT_KEY;

import com.tissue.shared.exception.base.ResourceConflictException;

public class DuplicateProjectKeyException extends ResourceConflictException {

    public DuplicateProjectKeyException(String projectKey) {
        super(ProjectErrorCode.DUPLICATE_PROJECT_KEY);
        addContext(PROJECT_KEY, projectKey);
    }
}
