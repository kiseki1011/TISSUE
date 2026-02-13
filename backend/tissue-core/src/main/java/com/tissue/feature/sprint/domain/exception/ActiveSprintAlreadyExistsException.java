package com.tissue.feature.sprint.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.PROJECT_KEY;
import static com.tissue.shared.exception.ErrorContextKeys.SPRINT_ID;
import static com.tissue.shared.exception.ErrorContextKeys.SPRINT_TITLE;

import com.tissue.shared.exception.base.ResourceConflictException;

public class ActiveSprintAlreadyExistsException extends ResourceConflictException {

    public ActiveSprintAlreadyExistsException(String projectKey, Long sprintId, String sprintTitle) {
        super(SprintErrorCode.ACTIVE_SPRINT_ALREADY_EXISTS);
        addContext(PROJECT_KEY, projectKey);
        addContext(SPRINT_ID, sprintId);
        addContext(SPRINT_TITLE, sprintTitle);
    }
}
