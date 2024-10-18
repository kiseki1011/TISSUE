package com.uranus.taskmanager.api.member.dto.response;

import com.uranus.taskmanager.api.member.domain.Member;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SignupResponse {

	private final String loginId;
	private final String email;

	public static SignupResponse from(Member member) {
		return SignupResponse.builder()
			.loginId(member.getLoginId())
			.email(member.getEmail())
			.build();
	}
}
