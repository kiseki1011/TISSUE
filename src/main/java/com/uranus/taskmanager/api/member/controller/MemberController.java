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
	 *  - 회원 정보 조회
	 *    - 나의 이메일, 비밀번호, 가입 날짜
	 *  - 비밀번호 변경
	 *  - 회원 탈퇴
	 *    - 7일 동안 PENDING 상태에 있다가, 탈퇴 취소 요청 없을 시 탈퇴 처리
	 *  - 참여하고 있는 워크스페이스 목록 조회
	 *  - 비밀번호 찾기 (세션 불필요)
	 * 	  - 가입한 이메일, 로그인 ID를 통한 비밀번호 찾기
	 * 	  - 기입한 로그인 ID, 이메일이 일치하면 이메일로 임시 비밀번호 보내기
	 */
	private final MemberService memberService;

	@ResponseStatus(HttpStatus.CREATED)
	@PostMapping("/signup")
	public ApiResponse<SignupResponse> signup(@Valid @RequestBody SignupRequest signupRequest) {
		SignupResponse signupResponse = memberService.signup(signupRequest);
		return ApiResponse.created("Signup Success", signupResponse);
	}

}
