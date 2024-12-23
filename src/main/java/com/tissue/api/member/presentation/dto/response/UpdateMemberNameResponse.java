package com.tissue.api.member.presentation.dto.response;

import java.time.LocalDateTime;

import com.tissue.api.member.domain.Member;

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
