package com.uranus.taskmanager.api.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.uranus.taskmanager.api.auth.LoginRequired;
import com.uranus.taskmanager.api.auth.SessionKey;
import com.uranus.taskmanager.api.auth.dto.request.LoginRequest;
import com.uranus.taskmanager.api.auth.dto.response.LoginResponse;
import com.uranus.taskmanager.api.auth.service.AuthenticationService;
import com.uranus.taskmanager.api.common.ApiResponse;

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

		/**
		 * 세션에 loginId 또는 email을 세션에 저장한다
		 * Todo:
		 * 아래의 케이스에 대한 처리가 필요하다.
		 * loginId가 null은 아니지만 DB에 없고 email을 통해 조회한 경우, 또는 그 반대의 케이스.
		 */
		HttpSession session = request.getSession();
		session.setAttribute(SessionKey.LOGIN_MEMBER, loginResponse.getLoginId());
		return ApiResponse.ok("Login Success", loginResponse);
	}

	@ResponseStatus(HttpStatus.NO_CONTENT)
	@LoginRequired
	@PostMapping("/logout")
	public ApiResponse<Void> logout(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
		return ApiResponse.okWithNoContent("Logout Success");
	}

}
