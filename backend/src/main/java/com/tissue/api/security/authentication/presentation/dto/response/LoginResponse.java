package com.tissue.api.security.authentication.presentation.dto.response;

import com.tissue.api.member.domain.model.Member;

import lombok.Builder;

@Builder
public record LoginResponse(
	Long memberId,
	String loginId,
	String email,
	String username
) {
	public static LoginResponse from(Member member) {
		return LoginResponse.builder()
			.memberId(member.getId())
			.loginId(member.getLoginId())
			.email(member.getEmail())
			.username(member.getUsername())
			.build();
	}
}
