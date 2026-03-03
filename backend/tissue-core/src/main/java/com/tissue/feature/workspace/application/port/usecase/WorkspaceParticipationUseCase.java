package com.tissue.feature.workspace.application.port.usecase;

import com.tissue.feature.workspace.application.dto.request.InviteToWorkspaceCommand;
import com.tissue.feature.workspace.application.dto.response.command.InviteMembersResponse;

public interface WorkspaceParticipationUseCase {

    InviteMembersResponse inviteToWorkspace(String workspaceKey, InviteToWorkspaceCommand cmd, Long actorMemberId);

    void kick(String workspaceKey, Long targetMemberId, Long actorMemberId);

    void leave(String workspaceKey, Long actorMemberId);
}
