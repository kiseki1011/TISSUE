package com.tissue.security.authentication.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenService jwtTokenService;

    /**
     * JWT Authentication.
     *
     * <p>Extracts JWT token from HTTP header, validates token and then creates authentication and
     * saves it in SecurityContext.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        SecurityContextHolder.clearContext();

        String token = extractTokenFromRequest(request);

        if (StringUtils.hasText(token)) {
            Authentication authentication = jwtTokenService.getAuthentication(token);

            if (authentication instanceof AbstractAuthenticationToken authToken) {
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            }

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // pass request to next filter
        filterChain.doFilter(request, response);
    }

    /**
     * Extract JWT token from HTTP header.
     *
     * <p>Header form - Authorization: Bearer {token} Remove "Bearer" prefix and return token
     * string.
     */
    private @Nullable String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        return null;
    }
}
