package com.tissue.api.member.presentation.dto.response;

import java.time.LocalDateTime;

import com.tissue.api.member.domain.Member;

public record SignupMemberResponse(
	Long memberId,
	LocalDateTime createdAt
) {
	public static SignupMemberResponse from(Member member) {
		return new SignupMemberResponse(
			member.getId(),
			member.getCreatedDate()
		);
	}
}
