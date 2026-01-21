package com.tissue.workspace.domain.exception;

import static com.tissue.global.exception.ContextKeys.TOKEN;
import static com.tissue.global.exception.ContextKeys.WORKSPACE_KEY;

import com.tissue.global.exception.base.ResourceNotFoundException;

public class WorkspaceInviteLinkNotFoundException extends ResourceNotFoundException {

    public WorkspaceInviteLinkNotFoundException(String workspaceKey, String token) {
        super(WorkspaceErrorCode.INVITE_LINK_NOT_FOUND);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext(TOKEN, token);
    }
}
