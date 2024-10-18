package com.uranus.taskmanager.api.authentication.dto.request;

import com.uranus.taskmanager.api.member.domain.Member;

import lombok.Builder;
import lombok.Getter;

@Getter
public class LoginMemberDto {

	private final String loginId;
	private final String email;

	@Builder
	public LoginMemberDto(String loginId, String email) {
		this.loginId = loginId;
		this.email = email;
	}

	public static LoginMemberDto from(Member member) {
		return LoginMemberDto.builder()
			.loginId(member.getLoginId())
			.email(member.getEmail())
			.build();
	}
}
