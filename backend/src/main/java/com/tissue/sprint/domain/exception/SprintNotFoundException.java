package com.tissue.sprint.domain.exception;

import com.tissue.common.exception.base.ResourceNotFoundException;

public class SprintNotFoundException extends ResourceNotFoundException {

	public static final String ID_MESSAGE = "Sprint not found with id '%d'.";
	public static final String MESSAGE = "Sprint not found with id '%d', project key '%s'.";

	public SprintNotFoundException(Long sprintId) {
		super(ID_MESSAGE.formatted(sprintId));
		addContext("sprintId", sprintId);
	}

	public SprintNotFoundException(Long sprintId, String projectKey, String workspaceKey) {
		super(MESSAGE.formatted(sprintId, projectKey));
		addContext("sprintId", sprintId);
		addContext("projectKey", projectKey);
		addContext("workspaceKey", workspaceKey);
	}
}
