package com.tissue.workspace.application.port.in;

import static com.tissue.security.authorization.project.ProjectSecurityExpressions.*;
import static com.tissue.security.authorization.workspace.WorkspaceSecurityExpressions.*;

import org.springframework.security.access.prepost.PreAuthorize;

import com.tissue.workspace.application.dto.request.InviteToProjectCommand;
import com.tissue.workspace.application.dto.request.InviteToWorkspaceCommand;
import com.tissue.workspace.application.dto.request.KickWorkspaceMemberCommand;
import com.tissue.workspace.application.dto.response.InviteMembersResponse;

public interface WorkspaceParticipationUseCase {

	@PreAuthorize(REQUIRES_WORKSPACE_ROLE_GRANT_PERMISSION)
	InviteMembersResponse inviteToWorkspace(InviteToWorkspaceCommand cmd);

	@PreAuthorize(REQUIRES_PROJECT_ADMIN)
	InviteMembersResponse inviteToProject(InviteToProjectCommand cmd);

	@PreAuthorize(REQUIRES_SELF_MODIFICATION)
	void leave(String workspaceKey, Long memberId);

	@PreAuthorize(REQUIRES_HIGHER_WORKSPACE_ROLE)
	void kick(KickWorkspaceMemberCommand cmd);
}
