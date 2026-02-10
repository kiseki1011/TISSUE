package com.tissue.feature.workspace.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.MEMBER_ID;
import static com.tissue.shared.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.shared.exception.base.ForbiddenException;

public class WorkspaceOwnershipRequiredException extends ForbiddenException {

    public WorkspaceOwnershipRequiredException(WorkspaceMember member) {
        super(WorkspaceErrorCode.WORKSPACE_OWNERSHIP_REQUIRED);
        addContext(WORKSPACE_KEY, member.getWorkspaceKey());
        addContext(MEMBER_ID, member.getMemberId());
        addContext("currentRole", member.getRole());
    }
}
