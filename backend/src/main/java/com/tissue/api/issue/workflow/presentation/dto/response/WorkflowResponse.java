package com.tissue.api.issue.workflow.presentation.dto.response;

import com.tissue.api.issue.workflow.domain.model.WorkflowDefinition;

public record WorkflowResponse(
	String workspaceCode,
	String key
) {
	public static WorkflowResponse from(WorkflowDefinition workflowDefinition) {
		return new WorkflowResponse(workflowDefinition.getWorkspaceCode(), workflowDefinition.getKey());
	}
}
