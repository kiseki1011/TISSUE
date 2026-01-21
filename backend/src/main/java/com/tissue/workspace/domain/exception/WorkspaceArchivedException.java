package com.tissue.workspace.domain.exception;

import static com.tissue.global.exception.ContextKeys.WORKSPACE_KEY;

import com.tissue.global.exception.base.BadRequestException;
import com.tissue.workspace.domain.Workspace;

public class WorkspaceArchivedException extends BadRequestException {

    public WorkspaceArchivedException(Workspace workspace) {
        super(WorkspaceErrorCode.WORKSPACE_ARCHIVED);
        addContext(WORKSPACE_KEY, workspace.getKey());
    }
}
