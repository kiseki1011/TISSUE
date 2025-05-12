package com.tissue.api.member.presentation.dto.response.command;

import com.tissue.api.member.domain.model.Member;

public record MemberResponse(
	Long memberId
) {
	public static MemberResponse from(Member member) {
		return new MemberResponse(member.getId());
	}
}
