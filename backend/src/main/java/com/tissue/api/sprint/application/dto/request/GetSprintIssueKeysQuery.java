package com.tissue.api.sprint.application.dto.request;

public record GetSprintIssueKeysQuery(
	String workspaceKey,
	String projectKey,
	Long sprintId
) {
}
