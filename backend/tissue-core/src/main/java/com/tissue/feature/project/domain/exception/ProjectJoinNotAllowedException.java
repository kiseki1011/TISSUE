package com.tissue.feature.project.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.PROJECT_KEY;
import static com.tissue.shared.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.shared.exception.base.ForbiddenException;

public class ProjectJoinNotAllowedException extends ForbiddenException {

    public ProjectJoinNotAllowedException(String workspaceKey, String projectKey) {
        super(ProjectErrorCode.PROJECT_JOIN_NOT_ALLOWED);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext(PROJECT_KEY, projectKey);
    }
}
