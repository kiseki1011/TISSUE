package com.tissue.workspace.domain.exception;

import static com.tissue.exception.ErrorContextKeys.TOKEN;
import static com.tissue.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.exception.base.BadRequestException;
import com.tissue.workspace.domain.WorkspaceInviteLink;

public class InvalidWorkspaceInviteLinkException extends BadRequestException {

    public InvalidWorkspaceInviteLinkException(WorkspaceInviteLink link) {
        super(WorkspaceErrorCode.INVALID_INVITE_LINK);
        addContext(WORKSPACE_KEY, link.getWorkspaceKey());
        addContext(TOKEN, link.getToken());
    }
}
