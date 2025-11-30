package com.tissue.api.sprint.application.dto.request;

public record GetSprintDetailQuery(
	String workspaceKey,
	String projectKey,
	Long sprintId
) {
}
