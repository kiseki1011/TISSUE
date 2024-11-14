package com.uranus.taskmanager.api.security.authentication.presentation.dto;

import com.uranus.taskmanager.api.member.domain.Member;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public class LoginMember {
	private Long id;
	private String loginId;
	private String email;

	@Builder
	public LoginMember(Long id, String loginId, String email) {
		this.id = id;
		this.loginId = loginId;
		this.email = email;
	}

	public static LoginMember from(Member member) {
		return LoginMember.builder()
			.id(member.getId())
			.loginId(member.getLoginId())
			.email(member.getEmail())
			.build();
	}
}
