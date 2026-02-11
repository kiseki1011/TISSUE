package com.tissue.feature.workspace.application.port.usecase;

import com.tissue.feature.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.feature.workspace.application.dto.request.InviteToWorkspaceCommand;
import com.tissue.feature.workspace.application.dto.response.command.InviteMembersResponse;

public interface WorkspaceParticipationUseCase {

    InviteMembersResponse inviteToWorkspace(InviteToWorkspaceCommand cmd, WorkspaceMemberContext actorContext);

    void kick(Long targetMemberId, WorkspaceMemberContext actorContext);

    void leave(WorkspaceMemberContext actorContext);
}
