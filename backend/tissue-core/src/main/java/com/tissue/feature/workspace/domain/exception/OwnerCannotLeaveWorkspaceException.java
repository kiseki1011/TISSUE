package com.tissue.feature.workspace.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.MEMBER_ID;
import static com.tissue.shared.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.shared.exception.base.BadRequestException;

public class OwnerCannotLeaveWorkspaceException extends BadRequestException {

    public OwnerCannotLeaveWorkspaceException(WorkspaceMember member) {
        super(WorkspaceErrorCode.OWNER_CANNOT_LEAVE_WORKSPACE);
        addContext(MEMBER_ID, member.getMemberId());
        addContext(WORKSPACE_KEY, member.getWorkspaceKey());
    }
}
