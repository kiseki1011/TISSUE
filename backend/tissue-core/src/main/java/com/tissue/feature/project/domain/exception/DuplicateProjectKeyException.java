package com.tissue.feature.project.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.PROJECT_KEY;
import static com.tissue.shared.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.shared.exception.base.ResourceConflictException;

public class DuplicateProjectKeyException extends ResourceConflictException {

    public DuplicateProjectKeyException(String workspaceKey, String projectKey) {
        super(ProjectErrorCode.DUPLICATE_PROJECT_KEY);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext(PROJECT_KEY, projectKey);
    }
}
