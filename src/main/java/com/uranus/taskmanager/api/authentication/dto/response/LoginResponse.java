package com.uranus.taskmanager.api.authentication.dto.response;

import com.uranus.taskmanager.api.member.domain.Member;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {

	private final String loginId;
	private final String email;

	public static LoginResponse from(Member member) {
		return LoginResponse.builder()
			.loginId(member.getLoginId())
			.email(member.getEmail())
			.build();
	}

}
