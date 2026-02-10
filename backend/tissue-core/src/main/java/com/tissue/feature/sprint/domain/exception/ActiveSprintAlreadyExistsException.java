package com.tissue.feature.sprint.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.PROJECT_KEY;

import com.tissue.feature.sprint.domain.Sprint;
import com.tissue.shared.exception.base.ResourceConflictException;

public class ActiveSprintAlreadyExistsException extends ResourceConflictException {

    public ActiveSprintAlreadyExistsException(String projectKey, Sprint activeSprint) {
        super(SprintErrorCode.ACTIVE_SPRINT_ALREADY_EXISTS);
        addContext(PROJECT_KEY, projectKey);
        addContext("activeSprintId", activeSprint.getId());
        addContext("activeSprintTitle", activeSprint.getTitle());
    }
}
