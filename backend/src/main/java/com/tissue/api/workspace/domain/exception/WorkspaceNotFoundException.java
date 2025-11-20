package com.tissue.api.workspace.domain.exception;

import com.tissue.api.common.exception.base.ResourceNotFoundException;

public class WorkspaceNotFoundException extends ResourceNotFoundException {

	private static final String MESSAGE = "Workspace not found with key '%s'";

	public WorkspaceNotFoundException(String workspaceKey) {
		super(MESSAGE.formatted(workspaceKey));
	}
}
