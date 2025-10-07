package com.tissue.api.workspacemember.application.dto;

import com.tissue.api.workspacemember.domain.model.enums.WorkspaceRole;

public record UpdateRoleCommand(
	String workspaceKey,
	Long targetMemberId,
	Long memberId,
	WorkspaceRole role
) {
}
