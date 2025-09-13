package com.tissue.api.sprint.presentation.dto.response;

import com.tissue.api.sprint.domain.model.Sprint;

public record SprintResponse(
	String workspaceCode,
	String sprintKey
) {
	public static SprintResponse from(Sprint sprint) {
		return new SprintResponse(sprint.getWorkspaceKey(), sprint.getKey());
	}
}
