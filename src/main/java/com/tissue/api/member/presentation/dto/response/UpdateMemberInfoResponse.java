package com.tissue.api.member.presentation.dto.response;

import java.time.LocalDateTime;

import com.tissue.api.member.domain.Member;

public record UpdateMemberInfoResponse(
	Long memberId,
	LocalDateTime updatedAt
) {
	public static UpdateMemberInfoResponse from(Member member) {
		return new UpdateMemberInfoResponse(
			member.getId(),
			member.getLastModifiedDate()
		);
	}
}
