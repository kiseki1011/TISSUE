package com.tissue.workflow.application.dto.response;

import com.tissue.workflow.domain.Workflow;

public record WorkflowCreateResponse(
	String workspaceKey,
	String projectKey,
	Long workflowId
) {
	public static WorkflowCreateResponse from(Workflow workflow) {
		return new WorkflowCreateResponse(
			workflow.getProject().getWorkspaceKey(),
			workflow.getProject().getKey(),
			workflow.getId()
		);
	}
}
