package com.tissue.feature.project.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.PROJECT_KEY;

import com.tissue.shared.exception.base.BadRequestException;

public class ProjectArchivedException extends BadRequestException {

    public ProjectArchivedException(String projectKey) {
        super(ProjectErrorCode.PROJECT_ARCHIVED);
        addContext(PROJECT_KEY, projectKey);
    }
}
