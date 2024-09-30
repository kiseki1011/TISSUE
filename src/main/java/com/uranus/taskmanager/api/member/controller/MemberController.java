package com.uranus.taskmanager.api.member.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.uranus.taskmanager.api.common.ApiResponse;
import com.uranus.taskmanager.api.member.dto.request.SignupRequest;
import com.uranus.taskmanager.api.member.dto.response.SignupResponse;
import com.uranus.taskmanager.api.member.service.MemberService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {
	/**
	 * Todo
	 * 회원 가입 - 새로운 멤버 등록
	 * 회원 정보 조회/수정
	 * 비밀번호 찾기 - 가입한 이메일 통한 비밀번호 찾기 (세션 불필요)
	 * 비밀번호 변경
	 * 회원 탈퇴
	 * 침여하고 있는 워크스페이스 목록 조회
	 */
	private final MemberService memberService;

	@ResponseStatus(HttpStatus.CREATED)
	@PostMapping("/signup")
	public ApiResponse<SignupResponse> signup(@Valid @RequestBody SignupRequest signupRequest) {
		SignupResponse signupResponse = memberService.signup(signupRequest);
		return ApiResponse.created("Signup Success", signupResponse);
	}

}
