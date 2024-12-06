package com.uranus.taskmanager.api.member.presentation.dto.response;

import java.time.LocalDateTime;

import com.uranus.taskmanager.api.member.domain.Member;

public record UpdateMemberNameResponse(
	Long memberId,
	LocalDateTime updatedAt
) {
	public static UpdateMemberNameResponse from(Member member) {
		return new UpdateMemberNameResponse(
			member.getId(),
			member.getLastModifiedDate()
		);
	}
}
