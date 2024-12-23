package com.tissue.api.member.presentation.dto.response;

import java.time.LocalDateTime;

import com.tissue.api.member.domain.Member;

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
