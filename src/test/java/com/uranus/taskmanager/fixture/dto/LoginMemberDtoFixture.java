package com.uranus.taskmanager.fixture.dto;

import com.uranus.taskmanager.api.authentication.dto.LoginMember;

public class LoginMemberDtoFixture {
	public LoginMember createLoginMemberDto(String loginId, String email) {
		return LoginMember.builder()
			.loginId(loginId)
			.email(email)
			.build();
	}
}
