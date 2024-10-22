package com.uranus.taskmanager.fixture.dto;

import com.uranus.taskmanager.api.authentication.dto.request.LoginMemberDto;

public class LoginMemberDtoFixture {
	public LoginMemberDto createLoginMemberDto(String loginId, String email) {
		return LoginMemberDto.builder()
			.loginId(loginId)
			.email(email)
			.build();
	}
}
