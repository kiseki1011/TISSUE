package com.tissue.workspace.application.port.in;

import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.workspace.application.dto.request.InviteToWorkspaceCommand;
import com.tissue.workspace.application.dto.response.command.InviteMembersResponse;

public interface WorkspaceParticipationUseCase {

    InviteMembersResponse inviteToWorkspace(InviteToWorkspaceCommand cmd, WorkspaceMemberContext actorContext);

    void kick(Long targetMemberId, WorkspaceMemberContext actorContext);

    void leave(WorkspaceMemberContext actorContext);
}
