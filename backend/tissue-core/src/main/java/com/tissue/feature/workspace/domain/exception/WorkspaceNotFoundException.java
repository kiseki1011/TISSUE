package com.tissue.feature.workspace.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.shared.exception.base.ResourceNotFoundException;

public class WorkspaceNotFoundException extends ResourceNotFoundException {

    public WorkspaceNotFoundException(String workspaceKey) {
        super(WorkspaceErrorCode.WORKSPACE_NOT_FOUND);
        addContext(WORKSPACE_KEY, workspaceKey);
    }
}
