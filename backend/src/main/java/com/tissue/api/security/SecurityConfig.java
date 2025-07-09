package com.tissue.api.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.tissue.api.security.authentication.ExceptionHandlerFilter;
import com.tissue.api.security.authentication.MemberUserDetailsService;
import com.tissue.api.security.authentication.jwt.JwtAuthenticationEntryPoint;
import com.tissue.api.security.authentication.jwt.JwtAuthenticationFilter;
import com.tissue.api.security.authorization.ApiAccessDeniedHandler;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final ExceptionHandlerFilter exceptionHandlerFilter;
	private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
	private final ApiAccessDeniedHandler apiAccessDeniedHandler;

	// TODO: URL pattern을 properties 또는 yaml로 분리
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.csrf(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

			// allow endpoints
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(
					"/api/v1/auth/login",
					"/api/v1/members/check-*",
					"/swagger-ui/**",
					"/actuator/**"
				).permitAll()
				.requestMatchers(HttpMethod.POST, "/api/v1/members").permitAll()
				.anyRequest().authenticated()
			)

			.exceptionHandling(ex -> ex
				.authenticationEntryPoint(jwtAuthenticationEntryPoint)
				.accessDeniedHandler(apiAccessDeniedHandler)
			)

			// place filters in order
			.addFilterBefore(exceptionHandlerFilter, UsernamePasswordAuthenticationFilter.class)
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public DaoAuthenticationProvider authenticationProvider(MemberUserDetailsService userDetailsService) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
		provider.setUserDetailsService(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder());
		return provider;
	}

	@Bean
	public AuthenticationManager authenticationManager(
		HttpSecurity http,
		DaoAuthenticationProvider provider
	) throws Exception {
		return http.getSharedObject(AuthenticationManagerBuilder.class)
			.authenticationProvider(provider)
			.build();
	}
}
