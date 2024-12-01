package com.uranus.taskmanager.api.workspacemember.presentation.dto.response;

import java.time.LocalDateTime;

import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceRole;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public class JoinWorkspaceResponse {

	private Long workspaceMemberId;
	private String workspaceCode;
	private LocalDateTime joinedAt;
	private String nickname;
	private WorkspaceRole workspaceRole;

	@Builder
	public JoinWorkspaceResponse(
		Long workspaceMemberId,
		String workspaceCode,
		LocalDateTime joinedAt,
		String nickname,
		WorkspaceRole workspaceRole
	) {
		this.workspaceMemberId = workspaceMemberId;
		this.workspaceCode = workspaceCode;
		this.joinedAt = joinedAt;
		this.nickname = nickname;
		this.workspaceRole = workspaceRole;
	}

	public static JoinWorkspaceResponse from(WorkspaceMember workspaceMember) {
		return JoinWorkspaceResponse.builder()
			.workspaceMemberId(workspaceMember.getId())
			.workspaceCode(workspaceMember.getWorkspaceCode())
			.joinedAt(workspaceMember.getCreatedDate())
			.nickname(workspaceMember.getNickname())
			.workspaceRole(workspaceMember.getRole())
			.build();
	}
}
