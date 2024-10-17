package com.uranus.taskmanager.fixture.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.uranus.taskmanager.api.authentication.dto.request.LoginRequest;
import com.uranus.taskmanager.api.authentication.service.AuthenticationService;

@Component
public class LoginFixture {

	@Autowired
	private AuthenticationService authenticationService;

	public void loginWithId(String loginId, String password) {
		LoginRequest loginRequest = LoginRequest.builder()
			.loginId(loginId)
			.password(password)
			.build();

		authenticationService.login(loginRequest);
	}

	// Todo: 로그아웃 픽스쳐
}
