package com.uranus.taskmanager.api.workspacemember.presentation.dto.response;

import java.time.LocalDateTime;

import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceRole;

public record UpdateRoleResponse(
	Long workspaceMemberId,
	WorkspaceRole role,
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
