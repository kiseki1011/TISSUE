package com.uranus.taskmanager.api.member.presentation.dto.response;

import java.time.LocalDateTime;

import com.uranus.taskmanager.api.member.domain.Member;

public record UpdateMemberEmailResponse(
	Long memberId,
	LocalDateTime updatedAt
) {
	public static UpdateMemberEmailResponse from(Member member) {
		return new UpdateMemberEmailResponse(
			member.getId(),
			member.getLastModifiedDate()
		);
	}
}
