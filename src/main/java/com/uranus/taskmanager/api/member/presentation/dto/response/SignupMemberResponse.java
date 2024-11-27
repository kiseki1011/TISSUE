package com.uranus.taskmanager.api.member.presentation.dto.response;

import com.uranus.taskmanager.api.member.domain.Member;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SignupMemberResponse {

	private final String loginId;
	private final String email;

	public static SignupMemberResponse from(Member member) {
		return SignupMemberResponse.builder()
			.loginId(member.getLoginId())
			.email(member.getEmail())
			.build();
	}
}
