package com.uranus.taskmanager.api.workspacemember.presentation.dto.response;

import java.time.LocalDateTime;

import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspacemember.WorkspaceRole;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public class JoinWorkspaceResponse {

	private Long workspaceId;
	private String workspaceCode;
	private LocalDateTime joinedAt;
	private String nickname;
	private WorkspaceRole workspaceRole;
	private boolean isAlreadyMember;

	@Builder
	public JoinWorkspaceResponse(
		Long workspaceId,
		String workspaceCode,
		LocalDateTime joinedAt,
		String nickname,
		WorkspaceRole workspaceRole,
		boolean isAlreadyMember
	) {
		this.workspaceId = workspaceId;
		this.workspaceCode = workspaceCode;
		this.joinedAt = joinedAt;
		this.nickname = nickname;
		this.workspaceRole = workspaceRole;
		this.isAlreadyMember = isAlreadyMember;
	}

	public static JoinWorkspaceResponse from(
		Workspace workspace,
		WorkspaceMember workspaceMember,
		boolean isAlreadyMember
	) {
		return JoinWorkspaceResponse.builder()
			.workspaceId(workspace.getId())
			.workspaceCode(workspace.getCode())
			.joinedAt(workspaceMember.getCreatedDate())
			.nickname(workspaceMember.getNickname())
			.workspaceRole(workspaceMember.getRole())
			.isAlreadyMember(isAlreadyMember)
			.build();
	}
}
