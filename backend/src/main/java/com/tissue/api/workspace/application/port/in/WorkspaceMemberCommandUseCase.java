package com.tissue.api.workspace.application.port.in;

import static com.tissue.api.security.authorization.WorkspaceSecurityExpressions.*;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.workspace.application.dto.request.AddPositionCommand;
import com.tissue.api.workspace.application.dto.request.AddTeamCommand;
import com.tissue.api.workspace.application.dto.request.RemovePositionCommand;
import com.tissue.api.workspace.application.dto.request.RemoveTeamCommand;
import com.tissue.api.workspace.application.dto.request.UpdateDisplayNameCommand;
import com.tissue.api.workspace.application.dto.request.UpdateRoleCommand;
import com.tissue.api.workspace.application.dto.response.WorkspaceMemberCommandResult;

public interface WorkspaceMemberCommandUseCase {

	@Transactional
	@PreAuthorize(REQUIRES_SELF_MODIFICATION)
	WorkspaceMemberCommandResult updateDisplayName(UpdateDisplayNameCommand cmd);

	@Transactional
	@PreAuthorize(REQUIRES_ADMIN + " AND " + REQUIRES_HIGHER_ROLE_THAN_TARGET)
	WorkspaceMemberCommandResult updateRole(UpdateRoleCommand cmd);

	@Transactional
	@PreAuthorize(REQUIRES_SELF_MODIFICATION + " OR " + REQUIRES_ADMIN)
	WorkspaceMemberCommandResult addPosition(AddPositionCommand cmd);

	@Transactional
	@PreAuthorize(REQUIRES_SELF_MODIFICATION + " OR " + REQUIRES_ADMIN)
	WorkspaceMemberCommandResult removePosition(RemovePositionCommand cmd);

	@Transactional
	@PreAuthorize(REQUIRES_SELF_MODIFICATION + " OR " + REQUIRES_ADMIN)
	WorkspaceMemberCommandResult addTeam(AddTeamCommand cmd);

	@Transactional
	@PreAuthorize(REQUIRES_SELF_MODIFICATION + " OR " + REQUIRES_ADMIN)
	WorkspaceMemberCommandResult removeTeam(RemoveTeamCommand cmd);
}
