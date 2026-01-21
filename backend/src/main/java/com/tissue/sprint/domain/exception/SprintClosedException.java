package com.tissue.sprint.domain.exception;

import static com.tissue.global.exception.ContextKeys.PROJECT_KEY;
import static com.tissue.global.exception.ContextKeys.SPRINT_ID;
import static com.tissue.global.exception.ContextKeys.WORKSPACE_KEY;

import com.tissue.global.exception.base.BadRequestException;
import com.tissue.sprint.domain.Sprint;

public class SprintClosedException extends BadRequestException {

    public SprintClosedException(Sprint sprint) {
        super(SprintErrorCode.SPRINT_ALREADY_CLOSED);
        addContext(SPRINT_ID, sprint.getId());
        addContext(PROJECT_KEY, sprint.getProjectKey());
        addContext(WORKSPACE_KEY, sprint.getWorkspaceKey());
    }
}
