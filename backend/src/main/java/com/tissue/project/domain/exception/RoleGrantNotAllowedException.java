package com.tissue.project.domain.exception;

import com.tissue.global.exception.base.ForbiddenException;
import com.tissue.project.domain.enums.ProjectRole;
import com.tissue.workspace.domain.enums.WorkspaceRole;

public class RoleGrantNotAllowedException extends ForbiddenException {

    public RoleGrantNotAllowedException(WorkspaceRole workspaceRole, ProjectRole projectRole) {
        super(ProjectErrorCode.ROLE_GRANT_NOT_ALLOWED);
        addContext("workspaceRole", workspaceRole);
        addContext("projectRole", projectRole);
    }
}
