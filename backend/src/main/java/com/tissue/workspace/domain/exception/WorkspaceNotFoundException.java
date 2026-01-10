package com.tissue.workspace.domain.exception;

import static com.tissue.global.exception.ContextKeys.WORKSPACE_KEY;

import com.tissue.global.exception.base.ResourceNotFoundException;

public class WorkspaceNotFoundException extends ResourceNotFoundException {

    public WorkspaceNotFoundException(String workspaceKey) {
        super(WorkspaceErrorCode.WORKSPACE_NOT_FOUND);
        addContext(WORKSPACE_KEY, workspaceKey);
    }
}
