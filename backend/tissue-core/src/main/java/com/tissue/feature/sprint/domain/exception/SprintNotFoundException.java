package com.tissue.feature.sprint.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.PROJECT_KEY;
import static com.tissue.shared.exception.ErrorContextKeys.SPRINT_ID;

import com.tissue.shared.exception.base.ResourceNotFoundException;

public class SprintNotFoundException extends ResourceNotFoundException {

    public SprintNotFoundException(String projectKey, Long sprintId) {
        super(SprintErrorCode.SPRINT_NOT_FOUND);
        addContext(PROJECT_KEY, projectKey);
        addContext(SPRINT_ID, sprintId);
    }
}
