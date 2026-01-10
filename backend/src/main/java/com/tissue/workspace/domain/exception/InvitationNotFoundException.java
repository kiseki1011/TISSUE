package com.tissue.workspace.domain.exception;

import static com.tissue.global.exception.ContextKeys.INVITATION_ID;
import static com.tissue.global.exception.ContextKeys.MEMBER_ID;

import com.tissue.global.exception.base.ResourceNotFoundException;

public class InvitationNotFoundException extends ResourceNotFoundException {

    public InvitationNotFoundException(Long invitationId, Long memberId) {
        super(WorkspaceErrorCode.INVITATION_NOT_FOUND);
        addContext(INVITATION_ID, invitationId);
        addContext(MEMBER_ID, memberId);
    }
}
