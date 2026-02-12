package com.tissue.feature.workspace.application.port.usecase;

import com.tissue.feature.workspace.application.dto.request.CreateWorkspaceInviteLinkCommand;
import com.tissue.feature.workspace.application.dto.response.command.WorkspaceMemberResponse;
import com.tissue.feature.workspace.application.dto.response.query.WorkspaceInviteLinkDetail;

public interface WorkspaceLinkUseCase {

    String createWorkspaceLink(String workspaceKey, CreateWorkspaceInviteLinkCommand cmd, Long memberId);

    void expireLink(String workspaceKey, String token, Long memberId);

    WorkspaceMemberResponse joinViaLink(String workspaceKey, String token, Long actorMemberId);

    WorkspaceInviteLinkDetail getLinkDetail(String workspaceKey, String token, Long memberId);

    // TODO: getWorkspaceLinks
    //  all active links for the workspace
}
