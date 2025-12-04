package com.tissue.api.project.domain.exception;

import com.tissue.api.common.exception.base.ResourceNotFoundException;

public class ProjectNotFoundException extends ResourceNotFoundException {

	public ProjectNotFoundException(String projectKey, String workspaceKey) {
		super("Project '%s' not found within workspace '%s'.".formatted(projectKey, workspaceKey));
		addContext("workspaceKey", workspaceKey);
		addContext("projectKey", projectKey);
	}

	public ProjectNotFoundException(Long projectId) {
		super("Project not found with id '%d'.".formatted(projectId));
		addContext("projectId", projectId);
	}
}