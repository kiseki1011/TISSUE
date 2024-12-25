package com.tissue.api.workspacemember.presentation.dto.request;

import com.tissue.api.workspacemember.domain.WorkspaceRole;

import jakarta.validation.constraints.NotNull;

public record UpdateRoleRequest(
	@NotNull(message = "Select a valid workspace role")
	WorkspaceRole updateWorkspaceRole
) {
}
