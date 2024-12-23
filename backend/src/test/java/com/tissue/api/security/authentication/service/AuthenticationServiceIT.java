package com.tissue.api.security.authentication.service;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.tissue.api.member.domain.repository.MemberRepository;
import com.tissue.api.member.presentation.dto.request.SignupMemberRequest;
import com.tissue.api.member.service.command.MemberCommandService;
import com.tissue.api.security.authentication.exception.InvalidLoginIdentityException;
import com.tissue.api.security.authentication.exception.InvalidLoginPasswordException;
import com.tissue.api.security.authentication.presentation.dto.request.LoginRequest;
import com.tissue.helper.ServiceIntegrationTestHelper;
import com.tissue.api.security.authentication.presentation.dto.response.LoginResponse;

class AuthenticationServiceIT extends ServiceIntegrationTestHelper {
	@Autowired
	private AuthenticationService authenticationService;
	@Autowired
	private MemberCommandService memberCommandService;
	@Autowired
	private MemberRepository memberRepository;

	@BeforeEach
	void setup() {
		databaseCleaner.execute();
	}

	@Test
	@DisplayName("가입된 멤버의 로그인ID로 로그인이 가능하다")
	void testLoginWithLoginId_success() {
		// given
		SignupMemberRequest signupMemberRequest = signupRequestDtoFixture.createSignupRequest(
			"testuser",
			"testuser@test.com",
			"password123!"
		);

		memberCommandService.signup(signupMemberRequest);

		LoginRequest loginRequest = LoginRequest.builder()
			.loginId("testuser")
			.password("password123!")
			.build();
		// when
		LoginResponse loginResponse = authenticationService.login(loginRequest);

		// then
		assertThat(loginResponse).isNotNull();
		assertThat(loginResponse.getLoginId()).isEqualTo("testuser");
	}

	@Test
	@DisplayName("가입된 멤버의 이메일로 로그인할 수 있다")
	void testLoginWithEmail_success() {
		// given
		SignupMemberRequest signupMemberRequest = signupRequestDtoFixture.createSignupRequest(
			"testuser",
			"testuser@test.com",
			"password123!"
		);

		memberCommandService.signup(signupMemberRequest);

		LoginRequest loginRequest = LoginRequest.builder()
			.email("testuser@test.com")
			.password("password123!")
			.build();
		// when
		LoginResponse loginResponse = authenticationService.login(loginRequest);

		// then
		assertThat(loginResponse).isNotNull();
		assertThat(loginResponse.getEmail()).isEqualTo("testuser@test.com");
	}

	@Test
	@DisplayName("로그인 시 로그인ID 또는 이메일을 조회할 수 없으면 InvalidLoginIdentityException 발생")
	void test3() {
		// given
		SignupMemberRequest signupMemberRequest = signupRequestDtoFixture.createSignupRequest(
			"testuser",
			"testuser@test.com",
			"password123!"
		);
		memberCommandService.signup(signupMemberRequest);

		LoginRequest loginRequest = LoginRequest.builder()
			.loginId("badtestuser")
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
		SignupMemberRequest signupMemberRequest = signupRequestDtoFixture.createSignupRequest(
			"testuser",
			"testuser@test.com",
			"password123!"
		);
		memberCommandService.signup(signupMemberRequest);

		LoginRequest loginRequest = LoginRequest.builder()
			.loginId("testuser")
			.password("wrongpassword123!")
			.build();

		// when & then
		assertThatThrownBy(() -> authenticationService.login(loginRequest))
			.isInstanceOf(InvalidLoginPasswordException.class);
	}
}
