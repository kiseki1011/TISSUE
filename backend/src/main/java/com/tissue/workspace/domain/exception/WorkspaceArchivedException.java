package com.tissue.workspace.domain.exception;

import static com.tissue.common.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.common.exception.base.BadRequestException;

public class WorkspaceArchivedException extends BadRequestException {

    public WorkspaceArchivedException(String workspaceKey) {
        super(WorkspaceErrorCode.WORKSPACE_ARCHIVED);
        addContext(WORKSPACE_KEY, workspaceKey);
    }
}
