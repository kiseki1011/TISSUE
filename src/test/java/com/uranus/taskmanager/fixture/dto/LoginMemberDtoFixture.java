package com.uranus.taskmanager.fixture.dto;

import com.uranus.taskmanager.api.authentication.dto.LoginMember;

public class LoginMemberDtoFixture {
	public LoginMember createLoginMemberDto(Long id, String loginId, String email) {
		return LoginMember.builder()
			.id(id)
			.loginId(loginId)
			.email(email)
			.build();
	}
}
