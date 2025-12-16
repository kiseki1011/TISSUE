package com.tissue.workspace.domain.exception;

import com.tissue.common.exception.base.ResourceNotFoundException;

public class WorkspaceNotFoundException extends ResourceNotFoundException {

	public WorkspaceNotFoundException(String workspaceKey) {
		super("Workspace not found with key '%s'".formatted(workspaceKey));
		addContext("workspaceKey", workspaceKey);
	}
}
