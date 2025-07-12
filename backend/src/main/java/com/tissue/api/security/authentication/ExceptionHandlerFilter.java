package com.tissue.api.security.authentication;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tissue.api.common.dto.ApiResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExceptionHandlerFilter extends OncePerRequestFilter {

	private final ObjectMapper objectMapper;

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		try {
			filterChain.doFilter(request, response);
		} catch (AuthenticationException | AccessDeniedException ex) {
			throw ex;
		} catch (Exception ex) {
			handleException(response, ex);
		}
	}

	private void handleException(
		HttpServletResponse response,
		Exception ex
	) throws IOException {

		log.error("Unexpected exception occured during the security filter chain process.", ex);

		HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
		String message = "An unexpected error occurred.";

		if (ex instanceof IllegalArgumentException) {
			status = HttpStatus.BAD_REQUEST;
			message = ex.getMessage();
		}

		ApiResponse<Void> apiResponse = ApiResponse.failWithNoContent(status, message);

		response.setStatus(status.value());
		response.setContentType("application/json;charset=UTF-8");
		objectMapper.writeValue(response.getWriter(), apiResponse);
	}
}
