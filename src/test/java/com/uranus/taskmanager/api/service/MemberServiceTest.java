package com.uranus.taskmanager.api.service;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.uranus.taskmanager.api.repository.MemberRepository;
import com.uranus.taskmanager.api.request.SignupRequest;
import com.uranus.taskmanager.api.response.SignupResponse;

@SpringBootTest
class MemberServiceTest {

	@Autowired
	private MemberService memberService;
	@Autowired
	private MemberRepository memberRepository;

	@Test
	@DisplayName("회원 가입 시 멤버가 저장된다")
	void test1() {
		// given
		SignupRequest signupRequest = SignupRequest.builder()
			.loginId("testuser")
			.password("testpassword1234!")
			.email("testemail@test.com")
			.build();

		// when
		SignupResponse signupResponse = memberService.signup(signupRequest);

		// then
		assertThat(signupResponse.getLoginId()).isEqualTo("testuser");
		assertThat(signupResponse.getEmail()).isEqualTo("testemail@test.com");

	}

}