package com.tissue.api.workflow.application.dto;

public record DeleteWorkflowCommand(
	String workspaceKey,
	String projectKey,
	Long id
) {
}
