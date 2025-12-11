package com.tissue.api.workflow.application.dto.request;

public record DeleteWorkflowCommand(
	String workspaceKey,
	String projectKey,
	Long workflowId
) {
}
