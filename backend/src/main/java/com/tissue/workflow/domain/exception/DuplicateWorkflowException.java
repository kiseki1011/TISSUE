package com.tissue.workflow.domain.exception;

import com.tissue.common.exception.base.ResourceConflictException;
import com.tissue.project.domain.Project;

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
