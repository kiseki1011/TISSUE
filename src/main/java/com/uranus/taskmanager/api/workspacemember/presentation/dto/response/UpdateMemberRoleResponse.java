package com.uranus.taskmanager.api.workspacemember.presentation.dto.response;

import java.time.LocalDateTime;

import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceRole;

public record UpdateMemberRoleResponse(
	Long workspaceMemberId,
	WorkspaceRole role,
	LocalDateTime updatedAt
) {
	public static UpdateMemberRoleResponse from(WorkspaceMember workspaceMember) {
		return new UpdateMemberRoleResponse(
			workspaceMember.getId(),
			workspaceMember.getRole(),
			workspaceMember.getLastModifiedDate()
		);
	}
}
