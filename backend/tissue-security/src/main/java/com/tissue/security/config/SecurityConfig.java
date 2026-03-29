package com.tissue.security.config;

import com.tissue.security.domain.TokenProvider;
import com.tissue.security.handler.ApiAccessDeniedHandler;
import com.tissue.security.handler.ApiAuthenticationEntryPoint;
import com.tissue.security.oauth2.CustomOAuth2UserService;
import com.tissue.security.oauth2.HttpCookieOAuth2AuthorizationRequestRepository;
import com.tissue.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.tissue.security.oauth2.OAuth2AuthenticationSuccessHandler;
import com.tissue.security.principal.MemberDetails;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final ApiAuthenticationEntryPoint apiAuthenticationEntryPoint;
    private final ApiAccessDeniedHandler apiAccessDeniedHandler;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oauth2AuthenticationFailureHandler;
    private final HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository;
    private final TissueSecurityProperties tissueSecurityProperties;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        String secret = tissueSecurityProperties.getJwt().getSecret();
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(secretKey).build();
    }

    /**
     * Stateless authentication converter that reconstructs MemberDetails from JWT.
     */
    @Bean
    public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName(TokenProvider.CLAIM_AUTHORITIES);
        authoritiesConverter.setAuthorityPrefix("");

        return new Converter<Jwt, AbstractAuthenticationToken>() {
            @Override
            public AbstractAuthenticationToken convert(Jwt jwt) {
                Collection<GrantedAuthority> authorities = authoritiesConverter.convert(jwt);

                Long memberId = Long.parseLong(jwt.getSubject());
                String email = jwt.getClaimAsString(TokenProvider.CLAIM_EMAIL);
                String username = jwt.getClaimAsString(TokenProvider.CLAIM_USERNAME);
                Boolean elevated = jwt.getClaim(TokenProvider.CLAIM_ELEVATED);

                MemberDetails memberDetails = new MemberDetails(memberId, email, username, authorities);
                if (Boolean.TRUE.equals(elevated)) {
                    memberDetails.grantElevated(true);
                }

                return new UsernamePasswordAuthenticationToken(memberDetails, null, authorities);
            }
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, ObjectProvider<ClientRegistrationRepository> clientRegistrations) throws Exception {
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
                                "/api/v1/members/password/**")
                        .permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/members/check-email",
                                "/api/v1/members/check-username",
                                "/api/v1/members/signup/**",
                                "/api/v1/members/password/**",
                                "/api/v1/system-info")
                        .permitAll()
                        .requestMatchers("/login/**", "/oauth2/**")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt ->
                                jwt.decoder(jwtDecoder()).jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(apiAuthenticationEntryPoint))
                .exceptionHandling(handler -> handler.authenticationEntryPoint(apiAuthenticationEntryPoint)
                        .accessDeniedHandler(apiAccessDeniedHandler));

        if (clientRegistrations.getIfAvailable() != null) {
            http.oauth2Login(
                    oauth2 -> oauth2.authorizationEndpoint(endpoint -> endpoint.baseUri("/api/v1/auth/social/login")
                                    .authorizationRequestRepository(cookieAuthorizationRequestRepository))
                            .redirectionEndpoint(endpoint -> endpoint.baseUri("/login/oauth2/code/*"))
                            .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                            .successHandler(oauth2AuthenticationSuccessHandler)
                            .failureHandler(oauth2AuthenticationFailureHandler));
        }

        return http.build();
    }

    private CorsConfigurationSource corsConfigurationSource() {
        List<String> allowedOrigins = tissueSecurityProperties.getCors().getAllowedOrigins();
        return request -> {
            CorsConfiguration config = new CorsConfiguration();
            config.setAllowedHeaders(Collections.singletonList("*"));
            config.setAllowedMethods(Collections.singletonList("*"));
            config.setAllowedOriginPatterns(allowedOrigins);
            config.setAllowCredentials(true);
            return config;
        };
    }
}
