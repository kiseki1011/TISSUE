package com.tissue.feature.workspace.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.MEMBER_ID;
import static com.tissue.shared.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.shared.exception.base.ForbiddenException;

public class WorkspaceAdminOrSelfRequiredException extends ForbiddenException {

    public WorkspaceAdminOrSelfRequiredException(String workspaceKey, Long targetMemberId) {
        super(WorkspaceErrorCode.WORKSPACE_ADMIN_OR_SELF_REQUIRED);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext(MEMBER_ID, targetMemberId);
    }
}
