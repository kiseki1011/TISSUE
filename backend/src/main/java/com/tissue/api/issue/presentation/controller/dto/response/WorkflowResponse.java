package com.tissue.api.issue.presentation.controller.dto.response;

import com.tissue.api.issue.domain.newmodel.WorkflowDefinition;

public record WorkflowResponse(
	String workspaceCode,
	String key
) {
	public static WorkflowResponse from(WorkflowDefinition workflowDefinition) {
		return new WorkflowResponse(workflowDefinition.getWorkspaceCode(), workflowDefinition.getKey());
	}
}
