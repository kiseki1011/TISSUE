package com.tissue.workspace.application.port.in;

import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.workspace.application.dto.request.CreateWorkspaceInviteLinkCommand;

public interface WorkspaceLinkUseCase {

    String createWorkspaceLink(CreateWorkspaceInviteLinkCommand cmd, WorkspaceMemberContext actorContext);

    void expireLink(String token, WorkspaceMemberContext actorContext);
}
