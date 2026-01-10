package com.tissue.project.domain.exception;

import static com.tissue.global.exception.ContextKeys.PROJECT_KEY;
import static com.tissue.global.exception.ContextKeys.WORKSPACE_KEY;

import com.tissue.global.exception.base.ForbiddenException;

public class ProjectJoinNotAllowedException extends ForbiddenException {

    public ProjectJoinNotAllowedException(String workspaceKey, String projectKey) {
        super(ProjectErrorCode.PROJECT_JOIN_NOT_ALLOWED);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext(PROJECT_KEY, projectKey);
    }
}
