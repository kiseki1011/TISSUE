package com.tissue.workspace.domain.exception;

import com.tissue.global.exception.base.BadRequestException;

public class CannotChangeRoleToOwnerException extends BadRequestException {

    public CannotChangeRoleToOwnerException() {
        super(WorkspaceErrorCode.CANNOT_CHANGE_ROLE_TO_OWNER);
    }
}
