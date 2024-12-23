package com.tissue.api.workspacemember.presentation.dto.response;

import java.time.LocalDateTime;

import com.tissue.api.workspacemember.domain.WorkspaceMember;

public record JoinWorkspaceResponse(
	Long workspaceMemberId,
	String workspaceCode,
	LocalDateTime joinedAt
) {
	public static JoinWorkspaceResponse from(WorkspaceMember workspaceMember) {
		return new JoinWorkspaceResponse(
			workspaceMember.getId(),
			workspaceMember.getWorkspaceCode(),
			workspaceMember.getCreatedDate()
		);
	}
}
