package com.tissue.workspace.domain.exception;

import static com.tissue.exception.ErrorContextKeys.INVITATION_ID;
import static com.tissue.exception.ErrorContextKeys.STATUS;

import com.tissue.exception.base.BadRequestException;
import com.tissue.workspace.domain.Invitation;

public class InvitationAlreadyProcessedException extends BadRequestException {

    public InvitationAlreadyProcessedException(Invitation invitation) {
        super(WorkspaceErrorCode.INVITATION_ALREADY_PROCESSED);
        addContext(INVITATION_ID, invitation.getId());
        addContext(STATUS, invitation.getStatus());
    }
}
