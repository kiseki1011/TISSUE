package com.tissue.api.workspacemember.presentation.dto;

import java.time.LocalDateTime;

import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.enums.WorkspaceRole;

import lombok.Builder;

@Builder
public record WorkspaceMemberDetail(
	Long workspaceMemberId,
	String nickname,
	WorkspaceRole workspaceRole,
	LocalDateTime joinedWorkspaceAt,
	LocalDateTime updatedAt
) {
	public static WorkspaceMemberDetail from(WorkspaceMember workspaceMember) {
		return WorkspaceMemberDetail.builder()
			.workspaceMemberId(workspaceMember.getId())
			.nickname(workspaceMember.getDisplayName())
			.workspaceRole(workspaceMember.getRole())
			.joinedWorkspaceAt(workspaceMember.getCreatedDate())
			.updatedAt(workspaceMember.getLastModifiedDate())
			.build();
	}
}
