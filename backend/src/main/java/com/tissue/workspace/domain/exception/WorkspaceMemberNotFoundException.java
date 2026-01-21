package com.tissue.workspace.domain.exception;

import static com.tissue.global.exception.ContextKeys.MEMBER_ID;
import static com.tissue.global.exception.ContextKeys.WORKSPACE_KEY;
import static com.tissue.global.exception.ContextKeys.WORKSPACE_MEMBER_ID;

import com.tissue.global.exception.base.ResourceNotFoundException;

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
