package com.tissue.workspace.domain.exception;

import static com.tissue.global.exception.ContextKeys.MEMBER_ID;
import static com.tissue.global.exception.ContextKeys.WORKSPACE_KEY;

import com.tissue.global.exception.base.ForbiddenException;

public class WorkspaceAdminOrSelfRequiredException extends ForbiddenException {

    public WorkspaceAdminOrSelfRequiredException(String workspaceKey, Long targetMemberId) {
        super(WorkspaceErrorCode.WORKSPACE_ADMIN_OR_SELF_REQUIRED);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext(MEMBER_ID, targetMemberId);
    }
}
