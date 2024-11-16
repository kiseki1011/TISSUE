package com.uranus.taskmanager.api.workspacemember.presentation.dto;

import java.time.LocalDateTime;

import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.workspacemember.WorkspaceRole;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;

import lombok.Builder;
import lombok.Getter;

@Getter
public class WorkspaceMemberDetail {

	private Long workspaceMemberId;
	private String loginId;
	private String email;
	private String nickname;
	private WorkspaceRole workspaceRole;
	private LocalDateTime joinedWorkspaceAt;

	@Builder
	public WorkspaceMemberDetail(Long workspaceMemberId, String loginId, String email, String nickname,
		WorkspaceRole workspaceRole, LocalDateTime joinedWorkspaceAt) {
		this.workspaceMemberId = workspaceMemberId;
		this.loginId = loginId;
		this.email = email;
		this.nickname = nickname;
		this.workspaceRole = workspaceRole;
		this.joinedWorkspaceAt = joinedWorkspaceAt;
	}

	public static WorkspaceMemberDetail from(WorkspaceMember workspaceMember) {
		Member member = workspaceMember.getMember();
		return WorkspaceMemberDetail.builder()
			.workspaceMemberId(workspaceMember.getId())
			.loginId(member.getLoginId())
			.email(member.getEmail())
			.nickname(workspaceMember.getNickname())
			.workspaceRole(workspaceMember.getRole())
			.joinedWorkspaceAt(workspaceMember.getCreatedDate())
			.build();
	}
}
