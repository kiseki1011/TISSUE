package com.tissue.feature.workspace.application.port.usecase;

import com.tissue.feature.workspace.application.dto.request.CreateWorkspaceInviteLinkCommand;
import com.tissue.feature.workspace.application.dto.response.command.WorkspaceMemberResponse;

public interface WorkspaceLinkCommandUseCase {

    String createWorkspaceLink(String workspaceKey, CreateWorkspaceInviteLinkCommand cmd, Long actorMemberId);

    void deleteLink(String workspaceKey, String token, Long actorMemberId);

    WorkspaceMemberResponse joinViaLink(String workspaceKey, String token, Long actorMemberId);
}
