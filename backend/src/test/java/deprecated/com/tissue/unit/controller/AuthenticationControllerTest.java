package deprecated.com.tissue.unit.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Locale;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.tissue.api.member.presentation.dto.request.PermissionRequest;
import com.tissue.api.security.authentication.presentation.dto.request.LoginRequest;
import com.tissue.api.security.authentication.presentation.dto.response.LoginResponse;

import deprecated.com.tissue.support.helper.ControllerTestHelper;

class AuthenticationControllerTest extends ControllerTestHelper {

	@Test
	@DisplayName("POST /auth/login - 로그인에 성공하면 OK를 기대하고, Access 토큰이 생성된다")
	void test1() throws Exception {
		// given
		LoginRequest loginRequest = LoginRequest.builder()
			.identifier("user123")
			.password("password123!")
			.build();

		when(authenticationService.login(any(LoginRequest.class))).thenReturn(LoginResponse.builder()
			.accessToken("mock-access-token")
			.refreshToken("mock-refresh-token")
			.build());

		// when & then
		mockMvc.perform((post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginRequest))))
			.andExpect(jsonPath("$.data.accessToken").isNotEmpty())
			.andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
			.andExpect(status().isOk())
			.andDo(print());
	}

	@Test
	@DisplayName("POST /auth/token/elevate - 업데이트 권한 요청에 성공하면 OK")
	void getUpdateAuthorization_success_OK() throws Exception {
		// given
		PermissionRequest request = new PermissionRequest("password1234!");

		// when & then
		mockMvc.perform(post("/api/v1/auth/token/elevate")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Permission granted."))
			.andDo(print());
	}

	@Test
	@DisplayName("POST /auth/login - 로그인 시 비밀번호 필드가 비어있으면 검증에 실패한다")
	void test2() throws Exception {
		// given
		LoginRequest loginRequest = LoginRequest.builder()
			.identifier("user123")
			.password("")
			.build();

		// when & then
		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.header("Accept-Language", "en")
				.content(objectMapper.writeValueAsString(loginRequest)))
			.andExpect(status().isBadRequest())
			.andExpect(
				jsonPath("$.data..message").value(messageSource.getMessage("valid.notblank", null, Locale.ENGLISH)))
			.andDo(print());
	}

	@Test
	@DisplayName("POST /auth/logout - 로그아웃 시 세션 무효화가 호출된다")
	void test3() throws Exception {
		// given
		// when & then
		mockMvc.perform(post("/api/v1/auth/logout"))
			.andExpect(status().isOk())
			.andDo(print());
	}

}
