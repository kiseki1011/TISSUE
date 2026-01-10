package com.tissue.workspace.domain.exception;

import static com.tissue.global.exception.ContextKeys.MEMBER_ID;
import static com.tissue.global.exception.ContextKeys.WORKSPACE_KEY;

import com.tissue.global.exception.base.ForbiddenException;
import com.tissue.workspace.domain.WorkspaceMember;

public class WorkspaceOwnershipRequiredException extends ForbiddenException {

    public WorkspaceOwnershipRequiredException(WorkspaceMember member) {
        super(WorkspaceErrorCode.WORKSPACE_OWNERSHIP_REQUIRED);
        addContext(WORKSPACE_KEY, member.getWorkspaceKey());
        addContext(MEMBER_ID, member.getMemberId());
        addContext("currentRole", member.getRole());
    }
}
