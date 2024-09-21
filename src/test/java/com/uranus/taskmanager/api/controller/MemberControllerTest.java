package com.uranus.taskmanager.api.controller;

import static org.junit.jupiter.params.provider.Arguments.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uranus.taskmanager.api.request.SignupRequest;
import com.uranus.taskmanager.api.service.MemberService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@WebMvcTest(MemberController.class)
class MemberControllerTest {
	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;
	@MockBean
	private MemberService memberService;

	@Test
	@DisplayName("회원 가입에 검증을 통과하면 OK를 기대한다")
	void test1() throws Exception {
		SignupRequest signupRequest = SignupRequest.builder()
			.loginId("testuser1234")
			.email("testemail@gmail.com")
			.password("Testpassword1234!")
			.build();
		String requestBody = objectMapper.writeValueAsString(signupRequest);

		mockMvc.perform(post("/api/v1/members/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isOk())
			.andDo(print());
	}

	@ParameterizedTest
	@CsvSource({
		"'testtesttesttesttest1', 'User ID must be alphanumeric and must be between 2 and 20 characters'",
		"'1', 'User ID must be alphanumeric and must be between 2 and 20 characters'",
		"'test!!', 'User ID must be alphanumeric and must be between 2 and 20 characters'",
		"'한글아이디', 'User ID must be alphanumeric and must be between 2 and 20 characters'",
		"'test1한글', 'User ID must be alphanumeric and must be between 2 and 20 characters'",
	})
	@DisplayName("회원 가입에 loginId는 영문과 숫자 조합에 2~20자를 지켜야한다")
	void test2(String loginId, String loginIdValidMsg) throws Exception {
		SignupRequest signupRequest = SignupRequest.builder()
			.loginId(loginId)
			.email("testemail@gmail.com")
			.password("Testpassword!")
			.build();
		String requestBody = objectMapper.writeValueAsString(signupRequest);

		mockMvc.perform(post("/api/v1/members/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.fieldErrors.loginId").value(loginIdValidMsg))
			.andDo(print());
	}

	@ParameterizedTest
	@CsvSource({
		"'test', 'The password must be alphanumeric "
			+ "including at least one special character and must be between 8 and 30 characters'",
		"'Test1234', 'The password must be alphanumeric "
			+ "including at least one special character and must be between 8 and 30 characters'",
		"'한글패스워드', 'The password must be alphanumeric "
			+ "including at least one special character and must be between 8 and 30 characters'",
		"'Test1234!한글', 'The password must be alphanumeric "
			+ "including at least one special character and must be between 8 and 30 characters'",
	})
	@DisplayName("회원 가입에 password는 하나 이상의 영문자, 숫자와 특수문자를 포함한 조합에 8~30자를 지켜야한다")
	void test3(String password, String passwordValidMsg) throws Exception {
		SignupRequest signupRequest = SignupRequest.builder()
			.loginId("testuser1234")
			.email("testemail@gmail.com")
			.password(password)
			.build();
		String requestBody = objectMapper.writeValueAsString(signupRequest);

		mockMvc.perform(post("/api/v1/members/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.fieldErrors.password")
				.value(passwordValidMsg))
			.andDo(print());
	}

	static Stream<Arguments> provideInvalidInputs() {
		return Stream.of(
			arguments(null, null, null), // null
			arguments("", "", ""),   // 빈 문자열
			arguments(" ", " ", " ")  // 공백
		);
	}

	@ParameterizedTest
	@MethodSource("provideInvalidInputs")
	@DisplayName("회원 가입에 loginId, email, password는 null, 공백, 빈 문자이면 안된다")
	void test4(String loginId, String email, String password) throws Exception {
		// given
		SignupRequest signupRequest = SignupRequest.builder()
			.loginId(loginId)
			.email(email)
			.password(password)
			.build();
		String requestBody = objectMapper.writeValueAsString(signupRequest);

		// when & then
		mockMvc.perform(post("/api/v1/members/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.fieldErrors.loginId").exists())
			.andExpect(jsonPath("$.fieldErrors.email").exists())
			.andExpect(jsonPath("$.fieldErrors.password").exists())
			.andDo(print());
	}

}