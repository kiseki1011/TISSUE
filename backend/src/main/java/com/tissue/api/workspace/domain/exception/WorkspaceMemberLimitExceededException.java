package com.tissue.api.workspace.domain.exception;

import com.tissue.api.common.exception.base.BadRequestException;

public class WorkspaceMemberLimitExceededException extends BadRequestException {

	public static final String MESSAGE = "Exceeded the max number(%d) of members you can add to a workspace.";

	public WorkspaceMemberLimitExceededException(String workspaceKey, int workspaceMemberLimit) {
		super(MESSAGE.formatted(workspaceMemberLimit));
		addContext("workspaceKey", workspaceKey);
	}
}
