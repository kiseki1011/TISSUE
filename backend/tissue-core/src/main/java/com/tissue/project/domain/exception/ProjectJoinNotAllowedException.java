package com.tissue.project.domain.exception;

import static com.tissue.exception.ErrorContextKeys.PROJECT_KEY;
import static com.tissue.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.exception.base.ForbiddenException;

public class ProjectJoinNotAllowedException extends ForbiddenException {

    public ProjectJoinNotAllowedException(String workspaceKey, String projectKey) {
        super(ProjectErrorCode.PROJECT_JOIN_NOT_ALLOWED);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext(PROJECT_KEY, projectKey);
    }
}
