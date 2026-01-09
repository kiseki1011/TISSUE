package com.tissue.project.domain.exception;

import com.tissue.global.exception.base.BadRequestException;
import com.tissue.project.domain.enums.ProjectRole;

public class InvalidDefaultJoinRoleException extends BadRequestException {

    public InvalidDefaultJoinRoleException(ProjectRole role) {
        super(ProjectErrorCode.INVALID_DEFAULT_JOIN_ROLE);
        addContext("defaultJoinRole", role);
    }
}
