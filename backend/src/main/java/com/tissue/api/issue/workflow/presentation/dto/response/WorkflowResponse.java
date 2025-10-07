package com.tissue.api.issue.workflow.presentation.dto.response;

import com.tissue.api.issue.workflow.domain.model.Workflow;

public record WorkflowResponse(
	String workspaceKey,
	Long id
) {
	public static WorkflowResponse from(Workflow workflow) {
		return new WorkflowResponse(workflow.getWorkspace().getKey(), workflow.getId());
	}
}
