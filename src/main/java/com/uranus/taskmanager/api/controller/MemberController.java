package com.uranus.taskmanager.api.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {
	/**
	 * Todo
	 * (세션 or Token이 필요)
	 * 로그아웃 - 로그인한 사용자의 세션 무효화 및 쿠키 삭제
	 * 회원 정보 조회/수정
	 * 비밀번호 찾기 - 가입한 이메일 통한 비밀번호 찾기 (세션 불필요)
	 * 비밀번호 변경
	 * 회원 탈퇴
	 * 침여하고 있는 워크스페이스 목록 조회
	 */
}
