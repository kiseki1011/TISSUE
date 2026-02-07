package com.tissue.workspace.domain.exception;

import static com.tissue.common.exception.ErrorContextKeys.MEMBER_ID;
import static com.tissue.common.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.common.exception.base.BadRequestException;
import com.tissue.workspace.domain.WorkspaceMember;

public class OwnerCannotLeaveWorkspaceException extends BadRequestException {

    public OwnerCannotLeaveWorkspaceException(WorkspaceMember member) {
        super(WorkspaceErrorCode.OWNER_CANNOT_LEAVE_WORKSPACE);
        addContext(MEMBER_ID, member.getMemberId());
        addContext(WORKSPACE_KEY, member.getWorkspaceKey());
    }
}
