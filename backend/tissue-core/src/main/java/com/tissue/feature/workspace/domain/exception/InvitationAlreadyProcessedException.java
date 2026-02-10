package com.tissue.feature.workspace.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.INVITATION_ID;
import static com.tissue.shared.exception.ErrorContextKeys.STATUS;

import com.tissue.feature.workspace.domain.Invitation;
import com.tissue.shared.exception.base.BadRequestException;

public class InvitationAlreadyProcessedException extends BadRequestException {

    public InvitationAlreadyProcessedException(Invitation invitation) {
        super(WorkspaceErrorCode.INVITATION_ALREADY_PROCESSED);
        addContext(INVITATION_ID, invitation.getId());
        addContext(STATUS, invitation.getStatus());
    }
}
