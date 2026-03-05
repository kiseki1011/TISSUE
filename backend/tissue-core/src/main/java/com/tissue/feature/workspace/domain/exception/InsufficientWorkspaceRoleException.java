package com.tissue.feature.workspace.domain.exception;

import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.shared.exception.base.ForbiddenException;

public class InsufficientWorkspaceRoleException extends ForbiddenException {

    public InsufficientWorkspaceRoleException(WorkspaceRole requiredRole) {
        super(WorkspaceErrorCode.INSUFFICIENT_WORKSPACE_ROLE);
        addContext("requiredWorkspaceRole", requiredRole.toString());
    }
}
