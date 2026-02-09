package com.tissue.project.domain.exception;

import static com.tissue.exception.ErrorContextKeys.PROJECT_KEY;
import static com.tissue.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.exception.base.ForbiddenException;

public class RequireProjectEditPermission extends ForbiddenException {

    public RequireProjectEditPermission(String workspaceKey, String projectKey) {
        super(ProjectErrorCode.PROJECT_EDIT_PERMISSION_REQUIRED);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext(PROJECT_KEY, projectKey);
    }
}
