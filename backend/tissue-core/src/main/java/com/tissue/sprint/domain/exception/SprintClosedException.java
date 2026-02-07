package com.tissue.sprint.domain.exception;

import static com.tissue.common.exception.ErrorContextKeys.PROJECT_KEY;
import static com.tissue.common.exception.ErrorContextKeys.SPRINT_ID;

import com.tissue.common.exception.base.BadRequestException;

public class SprintClosedException extends BadRequestException {

    public SprintClosedException(String projectKey, Long sprintId) {
        super(SprintErrorCode.SPRINT_ALREADY_CLOSED);
        addContext(PROJECT_KEY, projectKey);
        addContext(SPRINT_ID, sprintId);
    }
}
