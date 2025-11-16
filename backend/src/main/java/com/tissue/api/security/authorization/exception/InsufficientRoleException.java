package com.tissue.api.security.authorization.exception;

import com.tissue.api.common.exception.base.ForbiddenException;
import com.tissue.api.workspacemember.domain.model.enums.WorkspaceRole;

public class InsufficientRoleException extends ForbiddenException {

	public InsufficientRoleException(WorkspaceRole requiredRole, WorkspaceRole currentRole) {
		super("The minimum required workspace role is '%s'. The current role is '%s'."
			.formatted(requiredRole, currentRole));

		addContext("requiredRole", requiredRole);
		addContext("currentRole", currentRole);
	}
}
