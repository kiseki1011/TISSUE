package com.uranus.taskmanager.fixture.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.uranus.taskmanager.api.member.dto.request.SignupRequest;
import com.uranus.taskmanager.api.member.service.MemberService;

@Component
public class MemberServiceFixture {

	@Autowired
	private MemberService memberService;

	public void signup(String loginId, String email, String password) {
		SignupRequest signupRequest = SignupRequest.builder()
			.loginId(loginId)
			.email(email)
			.password(password)
			.build();

		memberService.signup(signupRequest);
	}

	/*
	 * Todo
	 *  - 탈퇴 픽스처
	 *  - 이메일 수정 픽스처
	 */
}
