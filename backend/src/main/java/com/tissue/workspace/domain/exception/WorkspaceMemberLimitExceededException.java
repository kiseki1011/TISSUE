package com.tissue.workspace.domain.exception;

import static com.tissue.global.exception.ContextKeys.LIMIT;
import static com.tissue.global.exception.ContextKeys.WORKSPACE_KEY;

import com.tissue.global.exception.base.BadRequestException;

public class WorkspaceMemberLimitExceededException extends BadRequestException {

    public WorkspaceMemberLimitExceededException(String workspaceKey, int limit) {
        super(WorkspaceErrorCode.WORKSPACE_MEMBER_LIMIT_EXCEEDED);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext(LIMIT, limit);
    }
}
