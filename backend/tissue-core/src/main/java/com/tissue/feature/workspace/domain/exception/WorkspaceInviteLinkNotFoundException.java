package com.tissue.feature.workspace.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.TOKEN;
import static com.tissue.shared.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.shared.exception.base.ResourceNotFoundException;

public class WorkspaceInviteLinkNotFoundException extends ResourceNotFoundException {

    public WorkspaceInviteLinkNotFoundException(String workspaceKey, String token) {
        super(WorkspaceErrorCode.INVITE_LINK_NOT_FOUND);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext(TOKEN, token);
    }
}
