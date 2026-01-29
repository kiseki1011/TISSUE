package com.tissue.workspace.domain.exception;

import static com.tissue.common.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.common.exception.base.BadRequestException;
import com.tissue.workspace.domain.Workspace;

public class WorkspaceArchivedException extends BadRequestException {

    public WorkspaceArchivedException(Workspace workspace) {
        super(WorkspaceErrorCode.WORKSPACE_ARCHIVED);
        addContext(WORKSPACE_KEY, workspace.getKey());
    }
}
