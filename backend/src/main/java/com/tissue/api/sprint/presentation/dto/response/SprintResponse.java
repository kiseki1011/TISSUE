package com.tissue.api.sprint.presentation.dto.response;

import com.tissue.api.sprint.domain.Sprint;

public record SprintResponse(
	String workspaceCode,
	String sprintKey
) {
	public static SprintResponse from(Sprint sprint) {
		return new SprintResponse(sprint.getWorkspaceCode(), sprint.getSprintKey());
	}
}
