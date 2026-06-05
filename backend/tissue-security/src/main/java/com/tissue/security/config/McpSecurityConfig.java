package com.tissue.security.config;

import com.tissue.security.application.service.PersonalAccessTokenService;
import com.tissue.security.filter.PatAuthenticationFilter;
import com.tissue.security.handler.ApiAccessDeniedHandler;
import com.tissue.security.handler.ApiAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security chain for the MCP endpoint.
 *
 * <p>It is path-scoped to {@code /mcp/**} and ordered ahead of the main security chain in {@link SecurityConfig}.
 * Agents authenticate by Personal Access Token (PAT) here while the rest of the API use JWT.
 */
@Configuration
@RequiredArgsConstructor
public class McpSecurityConfig {

    private final ApiAuthenticationEntryPoint apiAuthenticationEntryPoint;
    private final ApiAccessDeniedHandler apiAccessDeniedHandler;

    @Bean
    @Order(1)
    public SecurityFilterChain mcpSecurityFilterChain(
            HttpSecurity http, PersonalAccessTokenService personalAccessTokenService) throws Exception {
        PatAuthenticationFilter patAuthenticationFilter = new PatAuthenticationFilter(personalAccessTokenService);

        http.securityMatcher("/mcp/**")
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .addFilterBefore(patAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(handler -> handler.authenticationEntryPoint(apiAuthenticationEntryPoint)
                        .accessDeniedHandler(apiAccessDeniedHandler));

        return http.build();
    }
}
