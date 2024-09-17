package com.uranus.taskmanager.api.response;

import com.uranus.taskmanager.api.domain.member.Member;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SignupResponse {

	private final String loginId;
	private final String email;

	public static SignupResponse fromEntity(Member member) {
		return SignupResponse.builder()
			.loginId(member.getLoginId())
			.email(member.getEmail())
			.build();
	}
}
