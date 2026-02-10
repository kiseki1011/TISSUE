package com.tissue.feature.workspace.domain.exception;

import static com.tissue.feature.workspace.domain.exception.WorkspaceErrorCode.DUPLICATE_WORKSPACE_KEY;
import static com.tissue.shared.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.shared.exception.base.ResourceConflictException;

public class DuplicateWorkspaceKeyException extends ResourceConflictException {

    public DuplicateWorkspaceKeyException(String workspaceKey) {
        super(DUPLICATE_WORKSPACE_KEY);
        addContext(WORKSPACE_KEY, workspaceKey);
    }
}
