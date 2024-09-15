package com.uranus.taskmanager.api.response;

import com.uranus.taskmanager.api.domain.member.Member;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {

	private String userId;
	private String email;

	public static LoginResponse fromEntity(Member member) {
		return LoginResponse.builder()
			.userId(member.getUserId())
			.email(member.getEmail())
			.build();
	}

}
