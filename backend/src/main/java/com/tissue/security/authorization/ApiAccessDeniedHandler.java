package com.tissue.security.authorization;

import java.io.IOException;
import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiAccessDeniedHandler implements AccessDeniedHandler {

	private final ObjectMapper objectMapper;

	@Override
	public void handle(
		HttpServletRequest request,
		HttpServletResponse response,
		AccessDeniedException accessDeniedException
	) throws IOException {
		log.warn("Access denied. Reason: {}", accessDeniedException.getMessage());

		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
			HttpStatus.FORBIDDEN,
			"You do not have permission to access this resource"
		);
		problemDetail.setTitle("Forbidden");
		problemDetail.setInstance(URI.create(request.getRequestURI()));

		response.setStatus(HttpStatus.FORBIDDEN.value());
		response.setContentType("application/json;charset=UTF-8");
		objectMapper.writeValue(response.getWriter(), problemDetail);
	}
}
