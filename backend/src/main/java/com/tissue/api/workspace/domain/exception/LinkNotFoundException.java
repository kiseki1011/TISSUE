package com.tissue.api.workspace.domain.exception;

import com.tissue.api.common.exception.base.ResourceNotFoundException;

public class LinkNotFoundException extends ResourceNotFoundException {

	public LinkNotFoundException(String workspaceKey, String token) {
		super("Workspace ('%s') invite link was not found.".formatted(workspaceKey));
		addContext("workspaceKey", workspaceKey);
		addContext("token", token);
	}
}
