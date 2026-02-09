package com.tissue.workspace.domain.exception;

import static com.tissue.exception.ErrorContextKeys.MEMBER_ID;
import static com.tissue.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.exception.base.ResourceNotFoundException;

public class WorkspaceMemberNotFoundException extends ResourceNotFoundException {

    public WorkspaceMemberNotFoundException(String workspaceKey, Long memberId) {
        super(WorkspaceErrorCode.WORKSPACE_MEMBER_NOT_FOUND);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext(MEMBER_ID, memberId);
    }
}
