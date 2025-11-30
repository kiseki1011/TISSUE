package com.tissue.api.sprint.application.dto.request;

public record CompleteSprintCommand(
	String workspaceKey,
	String projectKey,
	Long sprintId
) {
}
