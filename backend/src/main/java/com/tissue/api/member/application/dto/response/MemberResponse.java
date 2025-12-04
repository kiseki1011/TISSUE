package com.tissue.api.member.application.dto.response;

import com.tissue.api.member.domain.Member;

public record MemberResponse(
	Long memberId
) {
	public static MemberResponse from(Member member) {
		return new MemberResponse(member.getId());
	}
}
