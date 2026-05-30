package com.tissue.security.config;

import com.tissue.security.domain.TokenProvider;
import com.tissue.security.domain.TokenType;
import com.tissue.security.handler.ApiAccessDeniedHandler;
import com.tissue.security.handler.ApiAuthenticationEntryPoint;
import com.tissue.security.principal.MemberDetails;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
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
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
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
    private final TissueSecurityProperties tissueSecurityProperties;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * System-role hierarchy so {@code hasRole('ADMIN')} (used by {@code @RequireSystemAdmin}) also
     * admits {@code SUPER_ADMIN}. {@code hasRole} otherwise matches the exact {@code ROLE_ADMIN}.
     */
    @Bean
    static RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy("ROLE_SUPER_ADMIN > ROLE_ADMIN\nROLE_ADMIN > ROLE_USER");
    }

    /**
     * Wires the {@link RoleHierarchy} into method-security SpEL ({@code @PreAuthorize}); a plain
     * {@code RoleHierarchy} bean is not applied to method security automatically.
     */
    @Bean
    static MethodSecurityExpressionHandler methodSecurityExpressionHandler(RoleHierarchy roleHierarchy) {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setRoleHierarchy(roleHierarchy);
        return handler;
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        String secret = tissueSecurityProperties.getJwt().getSecret();
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey).build();

        OAuth2TokenValidator<Jwt> validator = JwtValidators.createDefaultWithIssuer(TokenProvider.ISSUER);
        decoder.setJwtValidator(validator);

        return decoder;
    }

    /**
     * Reconstructs MemberDetails from JWT.
     */
    @Bean
    public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName(TokenProvider.CLAIM_AUTHORITIES);
        authoritiesConverter.setAuthorityPrefix("");

        return new Converter<Jwt, AbstractAuthenticationToken>() {
            @Override
            public AbstractAuthenticationToken convert(Jwt jwt) {
                String tokenType = jwt.getClaimAsString(TokenProvider.CLAIM_TOKEN_TYPE);
                if (!Objects.equals(tokenType, TokenType.ACCESS.getValue())) {
                    throw new InvalidBearerTokenException("Token type must be access");
                }

                Collection<GrantedAuthority> authorities = authoritiesConverter.convert(jwt);

                Long memberId = Long.parseLong(jwt.getSubject());
                String email = jwt.getClaimAsString(TokenProvider.CLAIM_EMAIL);
                String username = jwt.getClaimAsString(TokenProvider.CLAIM_USERNAME);

                MemberDetails memberDetails = new MemberDetails(memberId, email, username, authorities);

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
                                "/api/v1/auth/token:refresh",
                                "/api/v1/members/signup/**",
                                "/api/v1/members/signup:requestVerification",
                                "/api/v1/members/password/**",
                                "/api/v1/members:restore",
                                "/api/v1/projects/*/integrations/github/webhook")
                        .permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/members:checkEmail",
                                "/api/v1/members:checkUsername",
                                "/api/v1/members/signup/**",
                                "/api/v1/members/password/**",
                                "/api/v1/system-info")
                        .permitAll()
                        .requestMatchers("/v3/api-docs/**", "/apidocs", "/*.svg", "/*.png")
                        .permitAll()
                        .requestMatchers(
                                "/actuator/health", "/actuator/health/**", "/actuator/info", "/actuator/prometheus")
                        .permitAll()
                        .requestMatchers("/actuator/**")
                        .denyAll()
                        .anyRequest()
                        .authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt ->
                                jwt.decoder(jwtDecoder()).jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(apiAuthenticationEntryPoint))
                .exceptionHandling(handler -> handler.authenticationEntryPoint(apiAuthenticationEntryPoint)
                        .accessDeniedHandler(apiAccessDeniedHandler));

        return http.build();
    }

    private CorsConfigurationSource corsConfigurationSource() {
        List<String> allowedOrigins = tissueSecurityProperties.getCors().getAllowedOrigins();
        return request -> {
            CorsConfiguration config = new CorsConfiguration();
            config.setAllowedHeaders(List.of("*"));
            config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
            config.setAllowedOriginPatterns(allowedOrigins);
            config.setAllowCredentials(true);
            return config;
        };
    }
}
