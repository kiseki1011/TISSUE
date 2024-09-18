package com.uranus.taskmanager.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uranus.taskmanager.api.auth.LoginRequired;
import com.uranus.taskmanager.api.auth.SessionKey;
import com.uranus.taskmanager.api.request.LoginRequest;
import com.uranus.taskmanager.api.request.SignupRequest;
import com.uranus.taskmanager.api.response.LoginResponse;
import com.uranus.taskmanager.api.response.SignupResponse;
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
	 * (세션 or Token 불필요)
	 * 로그인 - 로그인하면 세션을 생성. 해당 세션ID를 클라에게 전달.
	 *          서버는 이후 클라가 보낸 쿠키를 사용해 세션ID 식별
	 * 회원 가입 - 새로운 멤버 등록
	 */
	/**
	 * Todo 2
	 * 로그아웃 - 세션 끝내기
	 * 회원 가입을 MemberController로 이동
	 */
	private final AuthService authService;

	@PostMapping("/signup")
	public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest signupRequest) {
		SignupResponse signupResponse = authService.signup(signupRequest);
		return ResponseEntity.status(HttpStatus.OK).body(signupResponse);
	}

	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest,
		HttpServletRequest request) {
		LoginResponse loginResponse = authService.login(loginRequest);

		/**
		 * 세션에 loginId 또는 email을 세션에 저장한다
		 * Todo:
		 * 아래의 케이스에 대한 처리가 필요하다.
		 * loginId가 null은 아니지만 DB에 없고 email을 통해 조회한 경우, 또는 그 반대의 케이스.
		 */
		HttpSession session = request.getSession();
		if (loginRequest.getLoginId() != null) {
			session.setAttribute(SessionKey.LOGIN_MEMBER, loginResponse.getLoginId());
		} else {
			session.setAttribute(SessionKey.LOGIN_MEMBER, loginResponse.getEmail());
		}

		return ResponseEntity.status(HttpStatus.OK).body(loginResponse);
	}

	@LoginRequired
	@PostMapping("/logout")
	public ResponseEntity<String> logout(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
		return ResponseEntity.ok("Logout Successful"); // Todo: 추후에 ApiResponse 클래스 만들고 리팩토링
	}

}