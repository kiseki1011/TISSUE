package com.tissue.feature.workspace.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.shared.exception.base.ForbiddenException;

public class WorkspaceRoleGrantNotAllowedException extends ForbiddenException {

    public WorkspaceRoleGrantNotAllowedException(String workspaceKey, WorkspaceRole grantRole) {
        super(WorkspaceErrorCode.ROLE_GRANT_NOT_ALLOWED);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext("grantRole", grantRole);
    }
}
