package com.tissue.api.workspace.exception;

import com.tissue.api.common.exception.type.ResourceNotFoundException;

public class WorkspaceNotFoundException extends ResourceNotFoundException {

	private static final String MESSAGE = "Workspace not found with code: %s";

	public WorkspaceNotFoundException(String workspaceCode) {
		super(String.format(MESSAGE, workspaceCode));
	}
}
