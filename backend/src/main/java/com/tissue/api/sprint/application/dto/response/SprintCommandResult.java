package com.tissue.api.sprint.application.dto.response;

import com.tissue.api.sprint.domain.Sprint;

public record SprintCommandResult(
	String workspaceKey,
	String projectKey,
	Long sprintId
) {
	public static SprintCommandResult from(Sprint sprint) {
		return new SprintCommandResult(sprint.getWorkspaceKey(), sprint.getProjectKey(), sprint.getId());
	}
}
