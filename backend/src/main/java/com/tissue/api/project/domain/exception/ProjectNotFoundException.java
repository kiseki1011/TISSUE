package com.tissue.api.project.domain.exception;

import com.tissue.api.common.exception.base.ResourceNotFoundException;

public class ProjectNotFoundException extends ResourceNotFoundException {

	public ProjectNotFoundException(String projectKey, String workspaceKey) {
		super("Project '%s' not found within workspace '%s'.".formatted(projectKey, workspaceKey));
		addContext("projectKey", projectKey);
		addContext("workspaceKey", workspaceKey);
	}

	public ProjectNotFoundException(Long projectId) {
		super("Project not found with id '%d'.".formatted(projectId));
		addContext("projectId", projectId);
	}
}