package com.tissue.api.security.authentication.service;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.tissue.api.common.exception.type.AuthenticationFailedException;
import com.tissue.api.member.domain.Member;
import com.tissue.api.member.domain.repository.MemberRepository;
import com.tissue.api.member.exception.MemberNotFoundException;
import com.tissue.api.member.service.command.MemberCommandService;
import com.tissue.api.security.authentication.presentation.dto.request.LoginRequest;
import com.tissue.api.security.authentication.presentation.dto.response.LoginResponse;
import com.tissue.helper.ServiceIntegrationTestHelper;

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
	void canLoginWithLoginId() {
		// given
		Member member = testDataFixture.createMember("tester"); // password: test1234!

		LoginRequest loginRequest = LoginRequest.builder()
			.identifier(member.getLoginId())
			.password("test1234!")
			.build();

		// when
		LoginResponse loginResponse = authenticationService.login(loginRequest);

		// then
		assertThat(loginResponse).isNotNull();
		assertThat(loginResponse.loginId()).isEqualTo(member.getLoginId());
	}

	@Test
	@DisplayName("가입된 멤버의 이메일로 로그인할 수 있다")
	void canLoginWithEmail() {
		// given
		Member member = testDataFixture.createMember("tester"); // password: test1234!

		LoginRequest loginRequest = LoginRequest.builder()
			.identifier(member.getEmail())
			.password("test1234!")
			.build();

		// when
		LoginResponse loginResponse = authenticationService.login(loginRequest);

		// then
		assertThat(loginResponse).isNotNull();
		assertThat(loginResponse.email()).isEqualTo(member.getEmail());
	}

	@Test
	@DisplayName("유효하지 않은 로그인ID 또는 이메일로 로그인할 수 없다")
	void cannotLoginWithInvalidLoginIdOrEmail() {
		// given
		Member member = testDataFixture.createMember("tester");

		LoginRequest loginRequest = LoginRequest.builder()
			.identifier("nottester")
			.password("test1234!")
			.build();

		// when & then
		assertThatThrownBy(() -> authenticationService.login(loginRequest))
			.isInstanceOf(MemberNotFoundException.class);
	}

	@Test
	@DisplayName("유효하지 않은 패스워드로 로그인할 수 없다")
	void cannotLoginWithInvalidPassword() {
		// given
		Member member = testDataFixture.createMember("tester"); // password: test1234!

		LoginRequest loginRequest = LoginRequest.builder()
			.identifier("tester")
			.password("wrongpassword123!")
			.build();

		// when & then
		assertThatThrownBy(() -> authenticationService.login(loginRequest))
			.isInstanceOf(AuthenticationFailedException.class);
	}
}
