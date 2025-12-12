package com.tissue.api.workspace.domain.exception;

import com.tissue.api.common.exception.base.BadRequestException;

public class WorkspaceArchivedException extends BadRequestException {

	public WorkspaceArchivedException(String workspaceKey) {
		super("Workspace '%s' is archived. Cannot modify.".formatted(workspaceKey));
		addContext("workspaceKey", workspaceKey);
	}
}
