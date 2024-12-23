package com.tissue.api.security.authentication.presentation.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import com.tissue.helper.ControllerTestHelper;

@TestPropertySource(properties = {"test.allow.isLogin=false"})
class NoLoginAuthenticationControllerTest extends ControllerTestHelper {

	@Test
	@DisplayName("POST /auth/logout - 로그인 하지 않은 상태에서 로그아웃 시도 시 예외가 발생한다")
	void test4() throws Exception {

		mockMvc.perform(post("/api/v1/auth/logout"))
			.andExpect(status().isUnauthorized())
			.andDo(print());
	}
}
