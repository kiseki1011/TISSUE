package com.tissue.api.workflow.application.dto.request;

public record ArchiveWorkflowCommand(
	String workspaceKey,
	String projectKey,
	Long id
) {
}
