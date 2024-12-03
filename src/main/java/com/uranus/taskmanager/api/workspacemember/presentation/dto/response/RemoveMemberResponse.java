package com.uranus.taskmanager.api.workspacemember.presentation.dto.response;

import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceRole;

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
	private Long memberId;
	private String nickname;
	private WorkspaceRole workspaceRole;

	@Builder
	public RemoveMemberResponse(Long memberId, String nickname, WorkspaceRole workspaceRole) {
		this.memberId = memberId;
		this.nickname = nickname;
		this.workspaceRole = workspaceRole;
	}

	public static RemoveMemberResponse from(Long memberId, WorkspaceMember workspaceMember) {
		return RemoveMemberResponse.builder()
			.memberId(memberId)
			.nickname(workspaceMember.getNickname())
			.workspaceRole(workspaceMember.getRole())
			.build();
	}
}
