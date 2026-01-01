package com.tissue.workspace.application.port.in;

import com.tissue.workspace.application.dto.in.InviteToProjectCommand;
import com.tissue.workspace.application.dto.in.InviteToWorkspaceCommand;
import com.tissue.workspace.application.dto.in.KickWorkspaceMemberCommand;
import com.tissue.workspace.application.dto.in.LeaveWorkspaceCommand;
import com.tissue.workspace.application.dto.out.command.InviteMembersResponse;

public interface WorkspaceParticipationUseCase {

    InviteMembersResponse inviteToWorkspace(InviteToWorkspaceCommand cmd);

    InviteMembersResponse inviteToProject(InviteToProjectCommand cmd);

    void leave(LeaveWorkspaceCommand cmd);

    void kick(KickWorkspaceMemberCommand cmd);
}
