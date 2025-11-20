package com.tissue.api.workspace.application.port.in;

import static com.tissue.api.security.authorization.WorkspaceSecurityExpressions.*;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.workspace.application.dto.request.InviteMembersCommand;
import com.tissue.api.workspace.application.dto.request.KickWorkspaceMemberCommand;
import com.tissue.api.workspace.application.dto.response.InviteMembersResult;
import com.tissue.api.workspace.application.dto.response.WorkspaceCommandResult;
import com.tissue.api.workspace.application.dto.response.WorkspaceMemberCommandResult;

public interface WorkspaceParticipationUseCase {

	// TODO: MEMBER 이상도 초대를 허용할까?
	@Transactional
	@PreAuthorize(REQUIRES_ADMIN)
	InviteMembersResult invite(InviteMembersCommand cmd);

	// WorkspaceMemberCommandResult acceptInvitation(AcceptInvitationCommand cmd);

	@Transactional
	WorkspaceMemberCommandResult join(String workspaceKey, Long memberId);

	@Transactional
	WorkspaceCommandResult leave(String workspaceKey, Long memberId);

	@Transactional
	@PreAuthorize(REQUIRES_ADMIN + " AND " + REQUIRES_HIGHER_ROLE_THAN_TARGET)
	WorkspaceMemberCommandResult kick(KickWorkspaceMemberCommand cmd);
}
