package com.tissue.api.security.authentication.jwt;

import java.io.IOException;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String AUTHORIZATION_HEADER = "Authorization";
	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtTokenProvider jwtTokenProvider;

	/**
	 * JWT Authentication
	 *
	 * 1. Extracts JWT token from HTTP header
	 * 2. Validates token
	 * 3. Creates Authentication and saves it in SecurityContext
	 */
	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {

		SecurityContextHolder.clearContext();

		String token = extractTokenFromRequest(request);

		if (StringUtils.hasText(token)) {
			Authentication authentication = jwtTokenProvider.getAuthentication(token);

			if (authentication instanceof AbstractAuthenticationToken authToken) {
				authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
			}

			SecurityContextHolder.getContext().setAuthentication(authentication);
		}

		// pass request to next filter
		filterChain.doFilter(request, response);
	}

	/**
	 * Extract JWT token from HTTP header
	 *
	 * Header form - Authorization: Bearer {token}
	 * Remove "Bearer" prefix and return token string
	 */
	private String extractTokenFromRequest(HttpServletRequest request) {
		String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
			return bearerToken.substring(BEARER_PREFIX.length());
		}

		return null;
	}

	/**
	 * Skip JWT authentication for certain endpoints
	 *
	 * - login API
	 * - token refresh API
	 * - signup API
	 * - resource duplication check APIs
	 * - public APIs (health, API docs, etc...)
	 */
	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getRequestURI();
		String method = request.getMethod();

		return path.startsWith("/api/v1/auth/")
			|| (path.equals("/api/v1/members") && "POST".equals(method))
			|| path.startsWith("/api/v1/members/check-")
			|| path.startsWith("/actuator/")
			|| path.startsWith("/swagger-ui/");
	}
}
