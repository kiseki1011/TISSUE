package com.tissue.workspace.application.port.in;

import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.workspace.application.dto.in.InviteToProjectCommand;
import com.tissue.workspace.application.dto.in.InviteToWorkspaceCommand;
import com.tissue.workspace.application.dto.in.KickWorkspaceMemberCommand;
import com.tissue.workspace.application.dto.out.command.InviteMembersResponse;

public interface WorkspaceParticipationUseCase {

    // TODO: inviteToWorkspace, inviteToProject는 따로 WorkspaceInvitationService로 분리할까?
    InviteMembersResponse inviteToWorkspace(InviteToWorkspaceCommand cmd);

    InviteMembersResponse inviteToProject(InviteToProjectCommand cmd);

    void leave(WorkspaceMemberContext actorContext);

    void kick(KickWorkspaceMemberCommand cmd);
}
