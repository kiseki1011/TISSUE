package com.tissue.api.workspace.exception;

import com.tissue.api.common.exception.base.ForbiddenException;
import com.tissue.api.workspacemember.domain.model.enums.WorkspaceRole;

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
