package com.tissue.workflow.domain.exception;

import com.tissue.common.exception.base.ResourceNotFoundException;

public class WorkflowNotFoundException extends ResourceNotFoundException {

	public WorkflowNotFoundException(Long workflowId, String projectKey, String workspaceKey) {
		super("Workflow was not found in workspace '%s' - project '%s', with workflow id '%d'."
			.formatted(workspaceKey, projectKey, workflowId));
		addContext("workspaceKey", workspaceKey);
		addContext("projectKey", projectKey);
		addContext("workflowId", workflowId);
	}

	public WorkflowNotFoundException(Long workflowId) {
		super("Workflow was not found with workflow id '%d'.".formatted(workflowId));
		addContext("workflowId", workflowId);
	}
}
