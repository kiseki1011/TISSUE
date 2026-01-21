package com.tissue.project.domain.exception;

import static com.tissue.global.exception.ContextKeys.PROJECT_ID;
import static com.tissue.global.exception.ContextKeys.PROJECT_KEY;
import static com.tissue.global.exception.ContextKeys.WORKSPACE_KEY;

import com.tissue.global.exception.base.ResourceNotFoundException;

public class ProjectNotFoundException extends ResourceNotFoundException {

    public ProjectNotFoundException(Long projectId) {
        super(ProjectErrorCode.PROJECT_NOT_FOUND);
        addContext(PROJECT_ID, projectId);
    }

    public ProjectNotFoundException(String workspaceKey, String projectKey) {
        super(ProjectErrorCode.PROJECT_NOT_FOUND);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext(PROJECT_KEY, projectKey);
    }
}
