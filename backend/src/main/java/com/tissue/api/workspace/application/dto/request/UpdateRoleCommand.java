package com.tissue.api.workspace.application.dto.request;

import com.tissue.api.workspace.domain.enums.WorkspaceRole;

public record UpdateRoleCommand(
	String workspaceKey,
	Long targetMemberId,
	Long memberId,
	WorkspaceRole role
) {
}
