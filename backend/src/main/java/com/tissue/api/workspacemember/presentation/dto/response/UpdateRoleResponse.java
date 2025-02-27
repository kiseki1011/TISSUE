package com.tissue.api.workspacemember.presentation.dto.response;

import java.time.LocalDateTime;

import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.WorkspaceRole;

public record UpdateRoleResponse(
	Long workspaceMemberId,
	WorkspaceRole updatedRole,
	LocalDateTime updatedAt
) {
	public static UpdateRoleResponse from(WorkspaceMember workspaceMember) {
		return new UpdateRoleResponse(
			workspaceMember.getId(),
			workspaceMember.getRole(),
			workspaceMember.getLastModifiedDate()
		);
	}
}
