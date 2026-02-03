package com.tissue.project.domain.exception;

import static com.tissue.common.exception.ErrorContextKeys.PROJECT_KEY;
import static com.tissue.common.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.common.exception.base.ForbiddenException;

public class RequireProjectEditPermission extends ForbiddenException {

    public RequireProjectEditPermission(String workspaceKey, String projectKey) {
        super(ProjectErrorCode.PROJECT_EDIT_PERMISSION_REQUIRED);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext(PROJECT_KEY, projectKey);
    }
}
