package com.tissue.sprint.domain.exception;

import static com.tissue.common.exception.ErrorContextKeys.PROJECT_KEY;
import static com.tissue.common.exception.ErrorContextKeys.SPRINT_ID;
import static com.tissue.common.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.common.exception.base.ResourceNotFoundException;
import com.tissue.project.domain.Project;

public class SprintNotFoundException extends ResourceNotFoundException {

    public SprintNotFoundException(Long sprintId, String projectKey) {
        super(SprintErrorCode.SPRINT_NOT_FOUND);
        addContext(SPRINT_ID, sprintId);
        addContext(PROJECT_KEY, projectKey);
    }

    public SprintNotFoundException(Long sprintId, Project project) {
        super(SprintErrorCode.SPRINT_NOT_FOUND);
        addContext(SPRINT_ID, sprintId);
        addContext(PROJECT_KEY, project.getKey());
        addContext(WORKSPACE_KEY, project.getWorkspaceKey());
    }
}
