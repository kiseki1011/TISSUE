package com.tissue.api.workspace.application.port.in;

import static com.tissue.api.security.authorization.ProjectSecurityExpressions.*;
import static com.tissue.api.security.authorization.SecurityKeyWords.*;
import static com.tissue.api.security.authorization.WorkspaceSecurityExpressions.*;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.workspace.application.dto.request.InviteToProjectCommand;
import com.tissue.api.workspace.application.dto.request.InviteToWorkspaceCommand;
import com.tissue.api.workspace.application.dto.request.KickWorkspaceMemberCommand;
import com.tissue.api.workspace.application.dto.response.InviteMembersResult;
import com.tissue.api.workspace.application.dto.response.WorkspaceCommandResult;
import com.tissue.api.workspace.application.dto.response.WorkspaceMemberCommandResult;

@Transactional
public interface WorkspaceParticipationUseCase {

	@PreAuthorize(REQUIRES_WORKSPACE_ADMIN + AND + REQUIRES_GRANTABLE_WORKSPACE_ROLE)
	InviteMembersResult inviteToWorkspace(InviteToWorkspaceCommand cmd);

	@PreAuthorize(REQUIRES_PROJECT_ADMIN)
	InviteMembersResult inviteToProject(InviteToProjectCommand cmd);

	WorkspaceCommandResult leave(String workspaceKey, Long memberId);

	@PreAuthorize(REQUIRES_WORKSPACE_ADMIN + AND + REQUIRES_HIGHER_WORKSPACE_ROLE)
	WorkspaceMemberCommandResult kick(KickWorkspaceMemberCommand cmd);
}
