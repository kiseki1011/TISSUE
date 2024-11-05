package com.uranus.taskmanager.api.authentication.dto;

import com.uranus.taskmanager.api.member.domain.Member;

import lombok.Builder;
import lombok.Getter;

@Getter
public class LoginMember {

	private final String loginId;
	private final String email;

	@Builder
	public LoginMember(String loginId, String email) {
		this.loginId = loginId;
		this.email = email;
	}

	public static LoginMember from(Member member) {
		return LoginMember.builder()
			.loginId(member.getLoginId())
			.email(member.getEmail())
			.build();
	}
}
