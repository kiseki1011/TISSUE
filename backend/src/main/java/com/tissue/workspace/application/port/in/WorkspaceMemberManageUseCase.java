package com.tissue.workspace.application.port.in;

import static com.tissue.security.authorization.SecurityKeyWords.*;
import static com.tissue.security.authorization.WorkspaceSecurityExpressions.*;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.workspace.application.dto.request.AddPositionCommand;
import com.tissue.workspace.application.dto.request.AddTeamCommand;
import com.tissue.workspace.application.dto.request.RemovePositionCommand;
import com.tissue.workspace.application.dto.request.RemoveTeamCommand;
import com.tissue.workspace.application.dto.request.UpdateDisplayNameCommand;
import com.tissue.workspace.application.dto.request.UpdateRoleCommand;

@Transactional
public interface WorkspaceMemberManageUseCase {

	@PreAuthorize(REQUIRES_SELF_MODIFICATION)
	void updateDisplayName(UpdateDisplayNameCommand cmd);

	@PreAuthorize(REQUIRES_WORKSPACE_ADMIN + AND + REQUIRES_HIGHER_WORKSPACE_ROLE)
	void updateRole(UpdateRoleCommand cmd);

	@PreAuthorize(REQUIRES_SELF_MODIFICATION + OR + REQUIRES_WORKSPACE_ADMIN)
	void addPosition(AddPositionCommand cmd);

	@PreAuthorize(REQUIRES_SELF_MODIFICATION + OR + REQUIRES_WORKSPACE_ADMIN)
	void removePosition(RemovePositionCommand cmd);

	@PreAuthorize(REQUIRES_SELF_MODIFICATION + OR + REQUIRES_WORKSPACE_ADMIN)
	void addTeam(AddTeamCommand cmd);

	@PreAuthorize(REQUIRES_SELF_MODIFICATION + OR + REQUIRES_WORKSPACE_ADMIN)
	void removeTeam(RemoveTeamCommand cmd);
}
