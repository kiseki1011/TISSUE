package com.uranus.taskmanager.api.workspacemember.presentation.dto.response;

import com.uranus.taskmanager.api.workspacemember.WorkspaceRole;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public class RemoveMemberResponse {

	/**
	 * Todo
	 *  - 추후에 포지션(Position)을 추가하면, 포지션도 응답으로 추가
	 */
	private String memberIdentifier;
	private String nickname;
	private WorkspaceRole workspaceRole;

	@Builder
	public RemoveMemberResponse(String memberIdentifier, String nickname, WorkspaceRole workspaceRole) {
		this.memberIdentifier = memberIdentifier;
		this.nickname = nickname;
		this.workspaceRole = workspaceRole;
	}

	public static RemoveMemberResponse from(String memberIdentifier, WorkspaceMember workspaceMember) {
		return RemoveMemberResponse.builder()
			.memberIdentifier(memberIdentifier)
			.nickname(workspaceMember.getNickname())
			.workspaceRole(workspaceMember.getRole())
			.build();
	}
}
