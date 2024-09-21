package com.uranus.taskmanager.api.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uranus.taskmanager.api.auth.SessionKey;
import com.uranus.taskmanager.api.exception.UserNotLoggedInException;
import com.uranus.taskmanager.api.request.LoginRequest;
import com.uranus.taskmanager.api.response.LoginResponse;
import com.uranus.taskmanager.api.service.AuthService;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;
	@MockBean
	private AuthService authService;

	@Test
	@DisplayName("로그인에 성공하면 200 OK를 기대하고, 세션에 로그인ID가 저장된다")
	void test1() throws Exception {
		// given
		MockHttpSession session = new MockHttpSession();
		LoginRequest loginRequest = LoginRequest.builder()
			.loginId("user123")
			.email("test@gmail.com")
			.password("password123!")
			.build();
		LoginResponse loginResponse = LoginResponse.builder()
			.loginId("user123")
			.email("test@gmail.com")
			.build();
		when(authService.login(any(LoginRequest.class))).thenReturn(loginResponse);

		// when & then
		mockMvc.perform((post("/api/v1/auth/login")
				.session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginRequest))))
			.andExpect(jsonPath("$.loginId").value("user123"))
			.andExpect(jsonPath("$.email").value("test@gmail.com"))
			.andExpect(status().isOk())
			.andDo(print());

		assertThat(session.getAttribute(SessionKey.LOGIN_MEMBER)).isEqualTo("user123");
	}

	@Test
	@DisplayName("로그인 시 비밀번호 필드의 빈 검증이 실패하면 400 BAD_REQUEST를 기대한다")
	void test2() throws Exception {
		// given
		LoginRequest loginRequest = LoginRequest.builder()
			.loginId("user123")
			.password("")
			.build();

		// when & then
		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginRequest)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.fieldErrors.password").value("Password must not be blank"))
			.andDo(print());

	}

	@Test
	@DisplayName("로그아웃 시 세션이 무효화된다")
	void test3() throws Exception {
		// given
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionKey.LOGIN_MEMBER, "user123");

		// when & then
		mockMvc.perform(post("/api/v1/auth/logout")
				.session(session))
			.andExpect(status().isNoContent())
			.andDo(print());

		assertThat(session.isInvalid()).isTrue();

	}

	@Test
	@DisplayName("로그인 하지 않은 상태에서 로그아웃 시도 시 UserNotLoggedInException 발생")
	void test4() throws Exception {
		// given

		// when & then
		mockMvc.perform(post("/api/v1/auth/logout"))
			.andExpect(status().isUnauthorized())
			.andExpect(result -> assertThat(result.getResolvedException()).isInstanceOf(UserNotLoggedInException.class))
			.andDo(print());

	}

}
