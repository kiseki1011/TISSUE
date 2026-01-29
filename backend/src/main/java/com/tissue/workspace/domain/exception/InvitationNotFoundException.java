package com.tissue.workspace.domain.exception;

import static com.tissue.common.exception.ErrorContextKeys.INVITATION_ID;
import static com.tissue.common.exception.ErrorContextKeys.MEMBER_ID;

import com.tissue.common.exception.base.ResourceNotFoundException;

public class InvitationNotFoundException extends ResourceNotFoundException {

    public InvitationNotFoundException(Long invitationId, Long memberId) {
        super(WorkspaceErrorCode.INVITATION_NOT_FOUND);
        addContext(INVITATION_ID, invitationId);
        addContext(MEMBER_ID, memberId);
    }
}
