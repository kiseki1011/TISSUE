package com.tissue.project.domain.exception;

import static com.tissue.global.exception.ContextKeys.PROJECT_KEY;
import static com.tissue.global.exception.ContextKeys.WORKSPACE_KEY;

import com.tissue.global.exception.base.ForbiddenException;
import com.tissue.project.domain.enums.ProjectRole;

public class RoleGrantNotAllowedException extends ForbiddenException {

    public RoleGrantNotAllowedException(String workspaceKey, String projectKey, ProjectRole grantRole) {
        super(ProjectErrorCode.ROLE_GRANT_NOT_ALLOWED);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext(PROJECT_KEY, projectKey);
        addContext("grantRole", grantRole);
    }
}
