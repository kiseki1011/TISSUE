package com.tissue.sprint.domain.exception;

import static com.tissue.exception.ErrorContextKeys.PROJECT_KEY;

import com.tissue.exception.base.ResourceConflictException;
import com.tissue.sprint.domain.Sprint;

public class ActiveSprintAlreadyExistsException extends ResourceConflictException {

    public ActiveSprintAlreadyExistsException(String projectKey, Sprint activeSprint) {
        super(SprintErrorCode.ACTIVE_SPRINT_ALREADY_EXISTS);
        addContext(PROJECT_KEY, projectKey);
        addContext("activeSprintId", activeSprint.getId());
        addContext("activeSprintTitle", activeSprint.getTitle());
    }
}
