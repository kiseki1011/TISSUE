package com.uranus.taskmanager.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uranus.taskmanager.api.request.SignupRequest;
import com.uranus.taskmanager.api.response.SignupResponse;
import com.uranus.taskmanager.api.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
	/**
	 * Todo
	 * (세션 or Token 불필요)
	 * 로그인 - 로그인하면 세션을 생성. 해당 세션ID를 클라에게 전달.
	 *          서버는 이후 클라가 보낸 쿠키를 사용해 세션ID 식별
	 * 회원 가입 - 새로운 멤버 등록
	 */

	private final AuthService authService;

	@PostMapping("/signup")
	public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest signupRequest) {
		SignupResponse signupResponse = authService.signup(signupRequest);
		return ResponseEntity.status(HttpStatus.OK).body(signupResponse);
	}
}
