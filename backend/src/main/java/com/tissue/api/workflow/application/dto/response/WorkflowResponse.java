package com.tissue.api.workflow.application.dto.response;

import com.tissue.api.workflow.domain.Workflow;

public record WorkflowResponse(
	String workspaceKey,
	String projectKey,
	Long workflowId
) {
	public static WorkflowResponse from(Workflow workflow) {
		return new WorkflowResponse(
			workflow.getProject().getWorkspaceKey(),
			workflow.getProject().getKey(),
			workflow.getId()
		);
	}
}
