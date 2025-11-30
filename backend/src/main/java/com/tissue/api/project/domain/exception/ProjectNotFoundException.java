package com.tissue.api.project.domain.exception;

import com.tissue.api.common.exception.base.ResourceNotFoundException;

public class ProjectNotFoundException extends ResourceNotFoundException {

	private static final String MESSAGE = "Project not found with project key '%s' and workspace key '%s'.";

	public ProjectNotFoundException(String projectKey, String workspaceKey) {
		super(MESSAGE.formatted(projectKey, workspaceKey));
		addContext("workspaceKey", workspaceKey);
		addContext("projectKey", projectKey);
	}
}
