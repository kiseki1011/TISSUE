package com.uranus.taskmanager.api.workspacemember.presentation.dto;

import java.time.LocalDateTime;

import com.uranus.taskmanager.api.workspacemember.WorkspaceRole;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;

import lombok.Builder;
import lombok.Getter;

@Getter
public class WorkspaceMemberDetail {

	private Long workspaceMemberId;
	private String nickname;
	private WorkspaceRole workspaceRole;
	private LocalDateTime joinedWorkspaceAt;

	@Builder
	public WorkspaceMemberDetail(
		Long workspaceMemberId,
		String nickname,
		WorkspaceRole workspaceRole,
		LocalDateTime joinedWorkspaceAt
	) {
		this.workspaceMemberId = workspaceMemberId;
		this.nickname = nickname;
		this.workspaceRole = workspaceRole;
		this.joinedWorkspaceAt = joinedWorkspaceAt;
	}

	public static WorkspaceMemberDetail from(WorkspaceMember workspaceMember) {
		return WorkspaceMemberDetail.builder()
			.workspaceMemberId(workspaceMember.getId())
			.nickname(workspaceMember.getNickname())
			.workspaceRole(workspaceMember.getRole())
			.joinedWorkspaceAt(workspaceMember.getCreatedDate())
			.build();
	}
}
