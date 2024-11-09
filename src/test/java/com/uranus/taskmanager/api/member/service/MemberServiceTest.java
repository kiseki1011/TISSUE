package com.uranus.taskmanager.api.member.service;

import static org.assertj.core.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.member.dto.request.SignupRequest;
import com.uranus.taskmanager.api.member.dto.response.SignupResponse;
import com.uranus.taskmanager.helper.ServiceIntegrationTestHelper;

class MemberServiceTest extends ServiceIntegrationTestHelper {

	@BeforeEach
	public void init() {
		databaseCleaner.execute();
	}

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

	@Test
	@DisplayName("회원 가입시 패스워드가 암호화 된다")
	void test2() {
		// given
		SignupRequest signupRequest = SignupRequest.builder()
			.loginId("testuser")
			.password("testpassword1234!")
			.email("testemail@test.com")
			.build();

		// when
		SignupResponse signupResponse = memberService.signup(signupRequest);
		Optional<Member> member = memberRepository.findByLoginId("testuser");
		assertThat(member).isPresent();
		String encodedPassword = member.get().getPassword();

		// then
		assertThat(passwordEncoder.matches("testpassword1234!", encodedPassword)).isTrue();
	}

	@Test
	@DisplayName("회원 가입시 입력한 패스워드와 암호화한 패스워드는 서로 달라야한다")
	void test3() {
		// given
		SignupRequest signupRequest = SignupRequest.builder()
			.loginId("testuser")
			.password("testpassword1234!")
			.email("testemail@test.com")
			.build();

		// when
		SignupResponse signupResponse = memberService.signup(signupRequest);
		Optional<Member> member = memberRepository.findByLoginId("testuser");
		assertThat(member).isPresent();
		String encodedPassword = member.get().getPassword();

		// then
		assertThat(encodedPassword).isNotEqualTo("testpassword1234!");
	}

}
