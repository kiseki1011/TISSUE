package com.uranus.taskmanager.api.authentication.controller;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.uranus.taskmanager.api.authentication.LoginRequired;
import com.uranus.taskmanager.api.authentication.SessionKey;
import com.uranus.taskmanager.api.authentication.dto.request.LoginRequest;
import com.uranus.taskmanager.api.authentication.dto.response.LoginResponse;
import com.uranus.taskmanager.api.authentication.service.AuthenticationService;
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

		HttpSession session = request.getSession();
		/*
		 * Todo
		 *  - 다음을 고려 중
		 *  - MemberSession을 만들어서 객체를 세션에 저장할까? -> 직렬화 필요!
		 *  - 지금 처럼 사용할 가능성이 높은 정보를 모두 저장할까? -> 특정 경우에 id를 통해 멤버를 찾는 과정을 생략할 수 있음
		 *  - 그냥 id만 저장할까?
		 */
		session.setAttribute(SessionKey.LOGIN_MEMBER_ID, loginResponse.getId());
		session.setAttribute(SessionKey.LOGIN_MEMBER_LOGIN_ID, loginResponse.getLoginId());
		session.setAttribute(SessionKey.LOGIN_MEMBER_EMAIL, loginResponse.getEmail());

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
