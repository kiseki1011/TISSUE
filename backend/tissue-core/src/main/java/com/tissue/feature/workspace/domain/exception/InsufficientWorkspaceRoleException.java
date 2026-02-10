package com.tissue.feature.workspace.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.shared.exception.base.ForbiddenException;

public class InsufficientWorkspaceRoleException extends ForbiddenException {

    public InsufficientWorkspaceRoleException(String workspaceKey, WorkspaceRole requiredRole) {
        super(WorkspaceErrorCode.INSUFFICIENT_WORKSPACE_ROLE);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext("requiredRole", requiredRole);
    }
}
