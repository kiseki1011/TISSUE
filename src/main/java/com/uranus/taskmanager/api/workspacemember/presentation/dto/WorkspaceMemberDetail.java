package com.uranus.taskmanager.api.workspacemember.presentation.dto;

import java.time.LocalDateTime;

import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceRole;

import lombok.Builder;
import lombok.Getter;

@Getter
public class WorkspaceMemberDetail {

	private Long workspaceMemberId;
	private String nickname;
	private WorkspaceRole workspaceRole;
	private LocalDateTime joinedWorkspaceAt;
	private LocalDateTime updatedAt;

	@Builder
	public WorkspaceMemberDetail(
		Long workspaceMemberId,
		String nickname,
		WorkspaceRole workspaceRole,
		LocalDateTime joinedWorkspaceAt,
		LocalDateTime updatedAt
	) {
		this.workspaceMemberId = workspaceMemberId;
		this.nickname = nickname;
		this.workspaceRole = workspaceRole;
		this.joinedWorkspaceAt = joinedWorkspaceAt;
		this.updatedAt = updatedAt;
	}

	public static WorkspaceMemberDetail from(WorkspaceMember workspaceMember) {
		return WorkspaceMemberDetail.builder()
			.workspaceMemberId(workspaceMember.getId())
			.nickname(workspaceMember.getNickname())
			.workspaceRole(workspaceMember.getRole())
			.joinedWorkspaceAt(workspaceMember.getCreatedDate())
			.updatedAt(workspaceMember.getLastModifiedDate())
			.build();
	}
}
