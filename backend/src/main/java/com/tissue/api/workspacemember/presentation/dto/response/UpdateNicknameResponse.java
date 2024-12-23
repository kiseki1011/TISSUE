package com.tissue.api.workspacemember.presentation.dto.response;

import java.time.LocalDateTime;

import com.tissue.api.workspacemember.domain.WorkspaceMember;

public record UpdateNicknameResponse(
	Long workspaceMemberId,
	String nickname,
	LocalDateTime updatedAt
) {
	public static UpdateNicknameResponse from(WorkspaceMember workspaceMember) {
		return new UpdateNicknameResponse(
			workspaceMember.getId(),
			workspaceMember.getNickname(),
			workspaceMember.getLastModifiedDate()
		);
	}
}
