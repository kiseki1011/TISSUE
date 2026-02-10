package com.tissue.feature.workspace.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.shared.exception.base.BadRequestException;

public class WorkspaceArchivedException extends BadRequestException {

    public WorkspaceArchivedException(String workspaceKey) {
        super(WorkspaceErrorCode.WORKSPACE_ARCHIVED);
        addContext(WORKSPACE_KEY, workspaceKey);
    }
}
