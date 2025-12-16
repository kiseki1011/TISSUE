package com.tissue.workspace.domain.exception;

import com.tissue.common.exception.base.ForbiddenException;
import com.tissue.workspace.domain.enums.WorkspaceRole;

public class WorkspaceOwnershipRequiredException extends ForbiddenException {

	public WorkspaceOwnershipRequiredException(
		String message,
		String workspaceKey,
		Long memberId,
		WorkspaceRole currentRole
	) {
		super(message);

		addContext("workspaceKey", workspaceKey);
		addContext("memberId", memberId);
		addContext("currentRole", currentRole.toString());
	}
}
