package com.tissue.workspace.domain.exception;

import static com.tissue.common.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.common.exception.base.ForbiddenException;
import com.tissue.workspace.domain.enums.WorkspaceRole;

public class WorkspaceRoleGrantNotAllowedException extends ForbiddenException {

    public WorkspaceRoleGrantNotAllowedException(String workspaceKey, WorkspaceRole grantRole) {
        super(WorkspaceErrorCode.ROLE_GRANT_NOT_ALLOWED);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext("grantRole", grantRole);
    }
}
