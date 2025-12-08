package com.tissue.api.workflow.domain.exception;

import com.tissue.api.common.exception.base.ResourceConflictException;

public class DuplicateWorkflowException extends ResourceConflictException {

	public DuplicateWorkflowException(String label, String projectKey, String workspaceKey) {
		super("Workflow with label '%s' already exists.".formatted(label));
		addContext("label", label);
		addContext("projectKey", projectKey);
		addContext("workspaceKey", workspaceKey);
	}
}
