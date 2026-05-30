package com.tissue.feature.project.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.PROJECT_KEY;

import com.tissue.shared.exception.base.ResourceNotFoundException;

public class ProjectNotFoundException extends ResourceNotFoundException {

    public ProjectNotFoundException(String projectKey) {
        super(ProjectErrorCode.PROJECT_NOT_FOUND);
        addContext(PROJECT_KEY, projectKey);
    }
}
