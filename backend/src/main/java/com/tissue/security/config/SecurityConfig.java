package com.tissue.security.config;

import com.tissue.security.authentication.infrastructure.oauth.CustomOAuth2UserService;
import com.tissue.security.authentication.infrastructure.persistence.HttpCookieOAuth2AuthorizationRequestRepository;
import com.tissue.security.authentication.presentation.OAuth2AuthenticationSuccessHandler;
import com.tissue.security.authentication.presentation.filter.ExceptionHandlerFilter;
import com.tissue.security.authentication.presentation.filter.JwtAuthenticationEntryPoint;
import com.tissue.security.authentication.presentation.filter.JwtAuthenticationFilter;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ExceptionHandlerFilter exceptionHandlerFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    // OAuth2 Dependencies
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler;
    private final HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(corsCustomizer -> corsCustomizer.configurationSource(corsConfigurationSource()))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/auth/login",
                                "/api/v1/auth/token",
                                "/api/v1/members/signup/**",
                                "/api/v1/members/verification/**")
                        .permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/members/checkEmail",
                                "/api/v1/members/checkUsername",
                                "/api/v1/members/verification/**",
                                "/api/v1/system-info")
                        .permitAll()
                        // OAuth2 related endpoints
                        .requestMatchers("/login/**", "/oauth2/**")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .oauth2Login(
                        oauth2 -> oauth2.authorizationEndpoint(endpoint -> endpoint.baseUri("/api/v1/auth/social/login")
                                        .authorizationRequestRepository(cookieAuthorizationRequestRepository))
                                .redirectionEndpoint(endpoint -> endpoint.baseUri("/login/oauth2/code/*"))
                                .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                                .successHandler(oauth2AuthenticationSuccessHandler))
                .exceptionHandling(handler -> handler.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(exceptionHandlerFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    private CorsConfigurationSource corsConfigurationSource() {
        return request -> {
            CorsConfiguration config = new CorsConfiguration();
            config.setAllowedHeaders(Collections.singletonList("*"));
            config.setAllowedMethods(Collections.singletonList("*"));
            config.setAllowedOriginPatterns(Collections.singletonList("*"));
            config.setAllowCredentials(true);
            return config;
        };
    }
}
