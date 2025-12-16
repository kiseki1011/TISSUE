package com.tissue.workspace.application.dto.request;

import com.tissue.workspace.domain.enums.WorkspaceRole;

public record UpdateRoleCommand(
	String workspaceKey,
	Long targetMemberId,
	Long memberId,
	WorkspaceRole role
) {
}
