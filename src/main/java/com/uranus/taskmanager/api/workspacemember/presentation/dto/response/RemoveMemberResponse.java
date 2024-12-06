package com.uranus.taskmanager.api.workspacemember.presentation.dto.response;

import java.time.LocalDateTime;

import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;

public record RemoveMemberResponse(
	/*
	 * Todo
	 *  - 추후에 포지션(Position)을 추가하면, 포지션도 응답으로 추가
	 */
	Long memberId,
	Long workspaceMemberId,
	LocalDateTime removedAt
) {
	public static RemoveMemberResponse from(Long memberId, WorkspaceMember workspaceMember) {
		return new RemoveMemberResponse(
			memberId,
			workspaceMember.getId(),
			workspaceMember.getLastModifiedDate()
		);
	}
}
