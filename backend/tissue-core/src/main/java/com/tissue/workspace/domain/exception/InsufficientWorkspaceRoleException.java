package com.tissue.workspace.domain.exception;

import static com.tissue.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.exception.base.ForbiddenException;
import com.tissue.workspace.domain.enums.WorkspaceRole;

public class InsufficientWorkspaceRoleException extends ForbiddenException {

    public InsufficientWorkspaceRoleException(String workspaceKey, WorkspaceRole requiredRole) {
        super(WorkspaceErrorCode.INSUFFICIENT_WORKSPACE_ROLE);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext("requiredRole", requiredRole);
    }
}
