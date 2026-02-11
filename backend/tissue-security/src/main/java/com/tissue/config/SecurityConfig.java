package com.tissue.config;

import com.tissue.domain.TokenProvider;
import com.tissue.handler.ApiAuthenticationEntryPoint;
import com.tissue.oauth2.CustomOAuth2UserService;
import com.tissue.oauth2.HttpCookieOAuth2AuthorizationRequestRepository;
import com.tissue.oauth2.OAuth2AuthenticationSuccessHandler;
import com.tissue.principal.MemberDetails;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler;
    private final HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository;

    @Value("${tissue.security.jwt.secret}")
    private String jwtSecret;

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
        SecretKeySpec secretKey = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(secretKey).build();
    }

    /**
     * Stateless authentication converter that reconstructs MemberDetails from JWT.
     * It delegates authority extraction to Spring's standard converter.
     * Uses Anonymous Class to avoid Generic type inference issues.
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

                String email = jwt.getSubject();
                Long memberId = jwt.getClaim(TokenProvider.CLAIM_MEMBER_ID);
                String username = jwt.getClaim(TokenProvider.CLAIM_USERNAME);
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
                                "/api/v1/members/check-email",
                                "/api/v1/members/check-username",
                                "/api/v1/members/verification/**",
                                "/api/v1/system-info")
                        .permitAll()
                        .requestMatchers("/login/**", "/oauth2/**")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt ->
                                jwt.decoder(jwtDecoder()).jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(apiAuthenticationEntryPoint))
                .oauth2Login(
                        oauth2 -> oauth2.authorizationEndpoint(endpoint -> endpoint.baseUri("/api/v1/auth/social/login")
                                        .authorizationRequestRepository(cookieAuthorizationRequestRepository))
                                .redirectionEndpoint(endpoint -> endpoint.baseUri("/login/oauth2/code/*"))
                                .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                                .successHandler(oauth2AuthenticationSuccessHandler))
                .exceptionHandling(handler -> handler.authenticationEntryPoint(apiAuthenticationEntryPoint));

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
