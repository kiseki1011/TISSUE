package com.tissue.workspace.domain.exception;

import static com.tissue.global.exception.ContextKeys.TOKEN;
import static com.tissue.global.exception.ContextKeys.WORKSPACE_KEY;

import com.tissue.global.exception.base.ForbiddenException;

public class InviteLinkEditNotAllowedException extends ForbiddenException {

    public InviteLinkEditNotAllowedException(String workspaceKey, String token) {
        super(WorkspaceErrorCode.INVITE_LINK_EDIT_NOT_ALLOWED);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext(TOKEN, token);
    }
}
