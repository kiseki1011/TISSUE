package com.tissue.workspace.domain.exception;

import static com.tissue.exception.ErrorContextKeys.LIMIT;
import static com.tissue.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.exception.base.BadRequestException;

public class WorkspaceMemberLimitExceededException extends BadRequestException {

    public WorkspaceMemberLimitExceededException(String workspaceKey, int limit) {
        super(WorkspaceErrorCode.WORKSPACE_MEMBER_LIMIT_EXCEEDED);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext(LIMIT, limit);
    }
}
