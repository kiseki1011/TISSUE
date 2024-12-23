package com.tissue.api.security.authentication.presentation.dto.response;

import java.time.LocalDateTime;

import com.tissue.api.member.domain.Member;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoginResponse {
	private Long memberId;
	private LocalDateTime loginAt;
	private String loginId;
	private String email;

	@Builder
	public LoginResponse(Long memberId, String loginId, String email) {
		this.memberId = memberId;
		this.loginAt = LocalDateTime.now();
		this.loginId = loginId;
		this.email = email;
	}

	public static LoginResponse from(Member member) {
		return LoginResponse.builder()
			.memberId(member.getId())
			.loginId(member.getLoginId())
			.email(member.getEmail())
			.build();
	}
}
