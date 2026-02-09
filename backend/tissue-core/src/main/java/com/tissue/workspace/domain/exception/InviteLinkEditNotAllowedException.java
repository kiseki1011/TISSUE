package com.tissue.workspace.domain.exception;

import static com.tissue.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.exception.base.ForbiddenException;

public class InviteLinkEditNotAllowedException extends ForbiddenException {

    public InviteLinkEditNotAllowedException(String workspaceKey, Long linkId) {
        super(WorkspaceErrorCode.INVITE_LINK_EDIT_NOT_ALLOWED);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext("linkId", linkId);
    }
}
