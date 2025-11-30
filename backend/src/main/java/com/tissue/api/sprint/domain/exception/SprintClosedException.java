package com.tissue.api.sprint.domain.exception;

import com.tissue.api.common.exception.base.BadRequestException;

public class SprintClosedException extends BadRequestException {

	public SprintClosedException(Long sprintId, String projectKey, String workspaceKey) {
		super("Cannot modify completed sprint(id= '%d').");
		addContext("sprintId", sprintId);
		addContext("projectKey", projectKey);
		addContext("workspaceKey", workspaceKey);
	}
}
