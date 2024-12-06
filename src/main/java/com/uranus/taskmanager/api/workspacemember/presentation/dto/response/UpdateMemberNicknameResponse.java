package com.uranus.taskmanager.api.workspacemember.presentation.dto.response;

import java.time.LocalDateTime;

import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;

public record UpdateMemberNicknameResponse(
	Long workspaceMemberId,
	String nickname,
	LocalDateTime updatedAt
) {
	public static UpdateMemberNicknameResponse from(WorkspaceMember workspaceMember) {
		return new UpdateMemberNicknameResponse(
			workspaceMember.getId(),
			workspaceMember.getNickname(),
			workspaceMember.getLastModifiedDate()
		);
	}
}
