package com.tissue.workspace.domain.exception;

import com.tissue.global.exception.base.InternalServerException;

public class WorkspaceKeyGenerationFailedException extends InternalServerException {

    public WorkspaceKeyGenerationFailedException(Throwable cause) {
        super(WorkspaceErrorCode.WORKSPACE_KEY_GENERATION_FAILED, cause);
    }
}
