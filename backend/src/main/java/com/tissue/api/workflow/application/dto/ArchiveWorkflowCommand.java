package com.tissue.api.workflow.application.dto;

public record ArchiveWorkflowCommand(
	String workspaceKey,
	String projectKey,
	Long id
) {
}
