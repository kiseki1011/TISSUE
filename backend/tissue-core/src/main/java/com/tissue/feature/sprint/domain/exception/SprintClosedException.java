package com.tissue.feature.sprint.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.PROJECT_KEY;
import static com.tissue.shared.exception.ErrorContextKeys.SPRINT_ID;

import com.tissue.shared.exception.base.BadRequestException;

public class SprintClosedException extends BadRequestException {

    public SprintClosedException(String projectKey, Long sprintId) {
        super(SprintErrorCode.SPRINT_ALREADY_CLOSED);
        addContext(PROJECT_KEY, projectKey);
        addContext(SPRINT_ID, sprintId);
    }
}
