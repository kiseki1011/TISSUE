package com.tissue.api.security.authentication.jwt;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tissue.api.common.dto.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ObjectMapper objectMapper;

	/**
	 * Is called if an unauthenticated user tries to access a protected endpoint or
	 * a AuthenticationException occurs during a security filter (401 Unauthorized).
	 */
	@Override
	public void commence(
		HttpServletRequest request,
		HttpServletResponse response,
		AuthenticationException authException
	) throws IOException {

		log.warn("Authentication exception occurred during a security filter process.");

		HttpStatus status = HttpStatus.UNAUTHORIZED;
		String message = "Authentication is required to access.";

		ApiResponse<Void> apiResponse = ApiResponse.failWithNoContent(status, message);

		response.setStatus(status.value());
		response.setContentType("application/json;charset=UTF-8");
		objectMapper.writeValue(response.getWriter(), apiResponse);
	}
}
