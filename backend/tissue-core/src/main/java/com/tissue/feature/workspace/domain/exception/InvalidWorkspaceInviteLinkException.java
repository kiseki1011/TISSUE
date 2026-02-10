package com.tissue.feature.workspace.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.TOKEN;
import static com.tissue.shared.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.feature.workspace.domain.WorkspaceInviteLink;
import com.tissue.shared.exception.base.BadRequestException;

public class InvalidWorkspaceInviteLinkException extends BadRequestException {

    public InvalidWorkspaceInviteLinkException(WorkspaceInviteLink link) {
        super(WorkspaceErrorCode.INVALID_INVITE_LINK);
        addContext(WORKSPACE_KEY, link.getWorkspaceKey());
        addContext(TOKEN, link.getToken());
    }
}
