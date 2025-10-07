package com.tissue.api.workspacemember.presentation.dto;

import java.time.Instant;

import com.tissue.api.workspacemember.domain.model.WorkspaceMember;
import com.tissue.api.workspacemember.domain.model.enums.WorkspaceRole;

import lombok.Builder;

@Builder
public record WorkspaceMemberDetail(
	Long workspaceMemberId,
	String nickname,
	WorkspaceRole workspaceRole,
	Instant joinedWorkspaceAt,
	Instant updatedAt
) {
	public static WorkspaceMemberDetail from(WorkspaceMember workspaceMember) {
		return WorkspaceMemberDetail.builder()
			.workspaceMemberId(workspaceMember.getId())
			.nickname(workspaceMember.getDisplayName())
			.workspaceRole(workspaceMember.getRole())
			.joinedWorkspaceAt(workspaceMember.getCreatedAt())
			.updatedAt(workspaceMember.getLastModifiedAt())
			.build();
	}
}
