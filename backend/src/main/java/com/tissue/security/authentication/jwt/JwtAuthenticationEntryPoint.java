package com.tissue.security.authentication.jwt;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

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
		log.warn("Authentication exception occurred during a security filter process");

		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
			HttpStatus.UNAUTHORIZED,
			"Authentication is required to access this resource."
		);
		problemDetail.setTitle("Unauthorized");
		problemDetail.setInstance(java.net.URI.create(request.getRequestURI()));

		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setContentType("application/json;charset=UTF-8");
		objectMapper.writeValue(response.getWriter(), problemDetail);
	}
}
