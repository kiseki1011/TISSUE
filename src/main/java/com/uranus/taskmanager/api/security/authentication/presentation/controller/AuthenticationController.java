package com.uranus.taskmanager.api.security.authentication.presentation.controller;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.uranus.taskmanager.api.common.ApiResponse;
import com.uranus.taskmanager.api.security.authentication.interceptor.LoginRequired;
import com.uranus.taskmanager.api.security.authentication.presentation.dto.request.LoginRequest;
import com.uranus.taskmanager.api.security.authentication.presentation.dto.response.LoginResponse;
import com.uranus.taskmanager.api.security.authentication.service.AuthenticationService;
import com.uranus.taskmanager.api.security.authentication.session.SessionAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

	private final AuthenticationService authenticationService;

	@PostMapping("/login")
	public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest,
		HttpServletRequest request) {

		LoginResponse loginResponse = authenticationService.login(loginRequest);

		HttpSession session = request.getSession();

		session.setAttribute(SessionAttributes.LOGIN_MEMBER_ID, loginResponse.getId());
		session.setAttribute(SessionAttributes.LOGIN_MEMBER_LOGIN_ID, loginResponse.getLoginId());
		session.setAttribute(SessionAttributes.LOGIN_MEMBER_EMAIL, loginResponse.getEmail());

		return ApiResponse.ok("Login Success", loginResponse);
	}

	@ResponseStatus(HttpStatus.NO_CONTENT)
	@LoginRequired
	@PostMapping("/logout")
	public ApiResponse<Void> logout(HttpServletRequest request) {

		HttpSession session = request.getSession(false);
		Optional.ofNullable(session)
			.ifPresent(HttpSession::invalidate);

		return ApiResponse.okWithNoContent("Logout Success");
	}

}
