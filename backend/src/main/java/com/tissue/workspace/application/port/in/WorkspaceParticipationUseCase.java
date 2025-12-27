package com.tissue.workspace.application.port.in;

import static com.tissue.project.application.service.authorization.ProjectAuthExpressions.*;
import static com.tissue.workspace.application.service.authorization.WorkspaceAuthExpressions.*;

import org.springframework.security.access.prepost.PreAuthorize;

import com.tissue.workspace.application.dto.in.InviteToProjectCommand;
import com.tissue.workspace.application.dto.in.InviteToWorkspaceCommand;
import com.tissue.workspace.application.dto.in.KickWorkspaceMemberCommand;
import com.tissue.workspace.application.dto.in.LeaveWorkspaceCommand;
import com.tissue.workspace.application.dto.out.command.InviteMembersResponse;

public interface WorkspaceParticipationUseCase {

	@PreAuthorize(REQUIRES_WORKSPACE_ROLE_GRANT_PERMISSION)
	InviteMembersResponse inviteToWorkspace(InviteToWorkspaceCommand cmd);

	@PreAuthorize(REQUIRES_PROJECT_ADMIN)
	InviteMembersResponse inviteToProject(InviteToProjectCommand cmd);

	@PreAuthorize(REQUIRES_SELF)
	void leave(LeaveWorkspaceCommand cmd);

	@PreAuthorize(REQUIRES_HIGHER_WORKSPACE_ROLE)
	void kick(KickWorkspaceMemberCommand cmd);
}
