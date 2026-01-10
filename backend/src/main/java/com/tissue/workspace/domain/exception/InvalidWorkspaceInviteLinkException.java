package com.tissue.workspace.domain.exception;

import static com.tissue.global.exception.ContextKeys.TOKEN;
import static com.tissue.global.exception.ContextKeys.WORKSPACE_KEY;

import com.tissue.global.exception.base.BadRequestException;
import com.tissue.workspace.domain.WorkspaceInviteLink;

public class InvalidWorkspaceInviteLinkException extends BadRequestException {

    public InvalidWorkspaceInviteLinkException(WorkspaceInviteLink link) {
        super(WorkspaceErrorCode.INVALID_INVITE_LINK);
        addContext(WORKSPACE_KEY, link.getWorkspaceKey());
        addContext(TOKEN, link.getToken());
    }
}
