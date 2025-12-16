package com.tissue.sprint.domain.exception;

import com.tissue.common.exception.base.ResourceConflictException;

public class ActiveSprintExistsException extends ResourceConflictException {

	public static final String MESSAGE = "A active sprint(id= '%d', title= '%s') already exists in project '%s'.";

	public ActiveSprintExistsException(String projectKey, Long sprintId, String sprintTitle) {
		super(MESSAGE.formatted(sprintId, sprintTitle, projectKey));
		addContext("projectKey", projectKey);
		addContext("sprintId", sprintId);
		addContext("sprintTitle", sprintTitle);
	}
}
