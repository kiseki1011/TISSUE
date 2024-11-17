package com.uranus.taskmanager.api.authentication.service;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.uranus.taskmanager.api.security.authentication.presentation.dto.request.LoginRequest;
import com.uranus.taskmanager.api.security.authentication.presentation.dto.response.LoginResponse;
import com.uranus.taskmanager.api.security.authentication.exception.InvalidLoginIdentityException;
import com.uranus.taskmanager.api.security.authentication.exception.InvalidLoginPasswordException;
import com.uranus.taskmanager.api.member.presentation.dto.request.SignupRequest;
import com.uranus.taskmanager.api.member.domain.repository.MemberRepository;
import com.uranus.taskmanager.api.member.service.MemberService;
import com.uranus.taskmanager.api.security.authentication.service.AuthenticationService;
import com.uranus.taskmanager.helper.ServiceIntegrationTestHelper;

class AuthenticationServiceTest extends ServiceIntegrationTestHelper {
	@Autowired
	private AuthenticationService authenticationService;
	@Autowired
	private MemberService memberService;
	@Autowired
	private MemberRepository memberRepository;

	@BeforeEach
	void setup() {
		databaseCleaner.execute();
	}

	@Test
	@DisplayName("가입된 멤버의 로그인ID로 로그인할 수 있다")
	void test1() {
		// given
		SignupRequest signupRequest = SignupRequest.builder()
			.loginId("user123")
			.email("user123@test.com")
			.password("password123!")
			.build();
		memberService.signup(signupRequest);

		LoginRequest loginRequest = LoginRequest.builder()
			.loginId("user123")
			.password("password123!")
			.build();
		// when
		LoginResponse loginResponse = authenticationService.login(loginRequest);

		// then
		assertThat(loginResponse).isNotNull();
		assertThat(loginResponse.getLoginId()).isEqualTo("user123");
	}

	@Test
	@DisplayName("가입된 멤버의 이메일로 로그인할 수 있다")
	void test2() {
		// given
		SignupRequest signupRequest = SignupRequest.builder()
			.loginId("user123")
			.email("user123@test.com")
			.password("password123!")
			.build();
		memberService.signup(signupRequest);

		LoginRequest loginRequest = LoginRequest.builder()
			.email("user123@test.com")
			.password("password123!")
			.build();
		// when
		LoginResponse loginResponse = authenticationService.login(loginRequest);

		// then
		assertThat(loginResponse).isNotNull();
		assertThat(loginResponse.getEmail()).isEqualTo("user123@test.com");
	}

	@Test
	@DisplayName("로그인 시 로그인ID 또는 이메일을 조회할 수 없으면 InvalidLoginIdentityException 발생")
	void test3() {
		// given
		SignupRequest signupRequest = SignupRequest.builder()
			.loginId("user123")
			.email("user123@test.com")
			.password("password123!")
			.build();
		memberService.signup(signupRequest);

		LoginRequest loginRequest = LoginRequest.builder()
			.loginId("baduser123")
			.password("password123!")
			.build();

		// when & then
		assertThatThrownBy(() -> authenticationService.login(loginRequest))
			.isInstanceOf(InvalidLoginIdentityException.class);
	}

	@Test
	@DisplayName("로그인 시 패스워드가 일치하지 않으면 InvalidLoginPasswordException 발생")
	void test4() {
		// given
		SignupRequest signupRequest = SignupRequest.builder()
			.loginId("user123")
			.email("user123@test.com")
			.password("password123!")
			.build();
		memberService.signup(signupRequest);

		LoginRequest loginRequest = LoginRequest.builder()
			.loginId("user123")
			.password("wrongpassword123!")
			.build();

		// when & then
		assertThatThrownBy(() -> authenticationService.login(loginRequest))
			.isInstanceOf(InvalidLoginPasswordException.class);
	}
}
