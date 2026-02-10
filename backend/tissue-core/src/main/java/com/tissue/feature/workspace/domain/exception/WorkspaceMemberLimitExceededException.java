package com.tissue.feature.workspace.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.LIMIT;
import static com.tissue.shared.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.shared.exception.base.BadRequestException;

public class WorkspaceMemberLimitExceededException extends BadRequestException {

    public WorkspaceMemberLimitExceededException(String workspaceKey, int limit) {
        super(WorkspaceErrorCode.WORKSPACE_MEMBER_LIMIT_EXCEEDED);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext(LIMIT, limit);
    }
}
