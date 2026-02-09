package com.tissue.workspace.application.port.in;

import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.workspace.application.dto.request.CreateWorkspaceInviteLinkCommand;
import com.tissue.workspace.application.dto.response.command.WorkspaceMemberResponse;
import com.tissue.workspace.application.dto.response.query.WorkspaceInviteLinkDetail;

public interface WorkspaceLinkUseCase {

    String createWorkspaceLink(CreateWorkspaceInviteLinkCommand cmd, WorkspaceMemberContext actorContext);

    void expireLink(String token, WorkspaceMemberContext actorContext);

    WorkspaceMemberResponse joinViaLink(String workspaceKey, String token, Long actorMemberId);

    WorkspaceInviteLinkDetail getLinkDetail(String token, WorkspaceMemberContext actor);

    // TODO: getWorkspaceLinks
    //  all active links for the workspace
}
