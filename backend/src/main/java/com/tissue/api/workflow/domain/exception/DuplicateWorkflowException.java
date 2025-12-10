package com.tissue.api.workflow.domain.exception;

import com.tissue.api.common.exception.base.ResourceConflictException;
import com.tissue.api.project.domain.Project;

public class DuplicateWorkflowException extends ResourceConflictException {

	public DuplicateWorkflowException(String label, Project project) {
		super("Workflow with label '%s' already exists.".formatted(label));
		addContext("label", label);
		addContext("projectKey", project.getKey());
		addContext("workspaceKey", project.getWorkspaceKey());
	}

	public DuplicateWorkflowException(String label, String projectKey, String workspaceKey) {
		super("Workflow with label '%s' already exists.".formatted(label));
		addContext("label", label);
		addContext("projectKey", projectKey);
		addContext("workspaceKey", workspaceKey);
	}
}
