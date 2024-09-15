package com.uranus.taskmanager.api.response;

import com.uranus.taskmanager.api.domain.member.Member;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SignUpResponse {

	private final String userId;
	private final String email;

	public static SignUpResponse fromEntity(Member member) {
		return SignUpResponse.builder()
			.userId(member.getUserId())
			.email(member.getEmail())
			.build();
	}
}
