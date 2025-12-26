package com.tissue.workspace.application.port.in;

import static com.tissue.security.authorization.workspace.WorkspaceSecurityExpressions.*;

import org.springframework.security.access.prepost.PreAuthorize;

import com.tissue.workspace.application.dto.in.ManagePositionCommand;
import com.tissue.workspace.application.dto.in.ManageTeamCommand;
import com.tissue.workspace.application.dto.in.UpdateDisplayNameCommand;
import com.tissue.workspace.application.dto.in.UpdateRoleCommand;

public interface WorkspaceMemberManageUseCase {

	@PreAuthorize(REQUIRES_SELF)
	void updateDisplayName(UpdateDisplayNameCommand cmd);

	@PreAuthorize(REQUIRES_HIGHER_WORKSPACE_ROLE)
	void updateRole(UpdateRoleCommand cmd);

	@PreAuthorize(REQUIRES_SELF)
	void addPosition(ManagePositionCommand cmd);

	@PreAuthorize(REQUIRES_SELF)
	void removePosition(ManagePositionCommand cmd);

	@PreAuthorize(REQUIRES_SELF)
	void addTeam(ManageTeamCommand cmd);

	@PreAuthorize(REQUIRES_SELF)
	void removeTeam(ManageTeamCommand cmd);
}
