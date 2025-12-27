package com.tissue.member.application.dto.response;

import com.tissue.member.domain.Member;

public record MemberSignupResponse(
	Long memberId
) {
	public static MemberSignupResponse from(Member member) {
		return new MemberSignupResponse(member.getId());
	}
}
