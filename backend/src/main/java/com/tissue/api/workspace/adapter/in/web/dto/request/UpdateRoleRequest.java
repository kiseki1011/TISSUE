package com.tissue.api.workspace.adapter.in.web.dto.request;

import com.tissue.api.workspace.domain.enums.WorkspaceRole;

import jakarta.validation.constraints.NotNull;

public record UpdateRoleRequest(
	@NotNull WorkspaceRole role
) {
}
