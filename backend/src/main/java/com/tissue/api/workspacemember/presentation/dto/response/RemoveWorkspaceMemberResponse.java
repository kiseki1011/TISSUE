package com.tissue.api.workspacemember.presentation.dto.response;

import java.time.LocalDateTime;

import com.tissue.api.workspacemember.domain.WorkspaceMember;

public record RemoveWorkspaceMemberResponse(
	/*
	 * Todo
	 *  - 추후에 포지션(Position)을 추가하면, 포지션도 응답으로 추가
	 */
	Long memberId,
	Long workspaceMemberId,
	LocalDateTime removedAt
) {
	public static RemoveWorkspaceMemberResponse from(Long memberId, WorkspaceMember workspaceMember) {
		return new RemoveWorkspaceMemberResponse(
			memberId,
			workspaceMember.getId(),
			workspaceMember.getLastModifiedDate()
		);
	}
}
