package com.tissue.unit.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import com.tissue.support.helper.ControllerTestHelper;

import jakarta.servlet.http.HttpServletResponse;

@ImportAutoConfiguration(SecurityAutoConfiguration.class)
@Import(NoLoginAuthenticationControllerTest.LogoutSecurityTestConfig.class)
class NoLoginAuthenticationControllerTest extends ControllerTestHelper {

	@TestConfiguration
	static class LogoutSecurityTestConfig {
		@Bean
		public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
			http
				.csrf(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.authorizeHttpRequests(auth -> auth
					.requestMatchers("/api/v1/auth/logout").authenticated()
					.anyRequest().permitAll()
				)
				.exceptionHandling(ex -> ex
					.authenticationEntryPoint((request, response, authException) -> {
						response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
					})
				);
			return http.build();
		}
	}

	@Test
	@DisplayName("POST /auth/logout - 로그인 하지 않은 상태에서 로그아웃 시도 시 401을 응답한다")
	void logout_whenNotAuthenticated_returnsUnauthorized() throws Exception {
		mockMvc.perform(post("/api/v1/auth/logout"))
			.andExpect(status().isUnauthorized())
			.andDo(print());
	}
}
