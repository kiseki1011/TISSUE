package com.tissue.api.security.authentication.presentation.dto.response;

import java.time.LocalDateTime;

import com.tissue.api.member.domain.Member;

import lombok.Builder;

@Builder
public record LoginResponse(
	Long memberId,
	LocalDateTime loginAt,
	String loginId,
	String email
) {
	public static LoginResponse from(Member member) {
		return LoginResponse.builder()
			.memberId(member.getId())
			.loginId(member.getLoginId())
			.email(member.getEmail())
			.build();
	}
}
