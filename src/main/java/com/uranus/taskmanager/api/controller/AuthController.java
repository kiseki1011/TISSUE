package com.uranus.taskmanager.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.uranus.taskmanager.api.auth.LoginRequired;
import com.uranus.taskmanager.api.auth.SessionKey;
import com.uranus.taskmanager.api.common.ApiResponse;
import com.uranus.taskmanager.api.request.LoginRequest;
import com.uranus.taskmanager.api.response.LoginResponse;
import com.uranus.taskmanager.api.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth") // Resource를 members가 아닌 auth로 표현하는 것이 좋을까?
@RequiredArgsConstructor
public class AuthController {
	/**
	 * Todo
	 * 로그인 - 로그인하면 세션을 생성. 해당 세션ID를 클라에게 전달.
	 *          서버는 이후 클라가 보낸 쿠키를 사용해 세션ID 식별
	 * 로그아웃 - 세션 끝내기
	 */
	private final AuthService authService;

	@PostMapping("/login")
	public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest,
		HttpServletRequest request) {
		LoginResponse loginResponse = authService.login(loginRequest);

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
