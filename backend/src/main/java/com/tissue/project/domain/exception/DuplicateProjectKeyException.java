package com.tissue.project.domain.exception;

import static com.tissue.global.exception.ContextKeys.PROJECT_KEY;
import static com.tissue.global.exception.ContextKeys.WORKSPACE_KEY;

import com.tissue.global.exception.base.ResourceConflictException;

public class DuplicateProjectKeyException extends ResourceConflictException {

    public DuplicateProjectKeyException(String workspaceKey, String projectKey) {
        super(ProjectErrorCode.DUPLICATE_PROJECT_KEY);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext(PROJECT_KEY, projectKey);
    }
}
