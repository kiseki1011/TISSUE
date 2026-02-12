package com.tissue.feature.workspace.application.port.usecase;

import com.tissue.feature.workspace.application.dto.request.InviteToWorkspaceCommand;
import com.tissue.feature.workspace.application.dto.response.command.InviteMembersResponse;

public interface WorkspaceParticipationUseCase {

    InviteMembersResponse inviteToWorkspace(String workspaceKey, InviteToWorkspaceCommand cmd, Long memberId);

    void kick(String workspaceKey, Long targetMemberId, Long memberId);

    void leave(String workspaceKey, Long memberId);
}
