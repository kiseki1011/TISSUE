package com.tissue.workspace.domain.exception;

import static com.tissue.common.exception.ErrorContextKeys.MEMBER_ID;
import static com.tissue.common.exception.ErrorContextKeys.WORKSPACE_KEY;
import static com.tissue.common.exception.ErrorContextKeys.WORKSPACE_MEMBER_ID;

import com.tissue.common.exception.base.ResourceNotFoundException;

public class WorkspaceMemberNotFoundException extends ResourceNotFoundException {

    public WorkspaceMemberNotFoundException(Long memberId, String workspaceKey) {
        super(WorkspaceErrorCode.WORKSPACE_MEMBER_NOT_FOUND);
        addContext(MEMBER_ID, memberId);
        addContext(WORKSPACE_KEY, workspaceKey);
    }

    public WorkspaceMemberNotFoundException(Long workspaceMemberId) {
        super(WorkspaceErrorCode.WORKSPACE_MEMBER_NOT_FOUND);
        addContext(WORKSPACE_MEMBER_ID, workspaceMemberId);
    }
}
