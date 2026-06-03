package com.tissue.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.tissue.security.config.TissueSecurityProperties;
import com.tissue.security.domain.exception.TokenExpiredException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class JwtTokenProviderTest {

    private static final String SECRET = "a".repeat(32);
    private static final Long MEMBER_ID = 1L;
    private static final String EMAIL = "user@test.com";
    private static final String USERNAME = "testuser";

    private final JwtTokenProvider tokenProvider = createTokenProvider();

    private static SecretKeySpec key(String secret) {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    private static JwtEncoder encoder(String secret) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key(secret)));
    }

    private static JwtDecoder decoder(String secret) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key(secret)).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("TISSUE"));
        return decoder;
    }

    private static JwtTokenProvider createTokenProvider() {
        TissueSecurityProperties properties = new TissueSecurityProperties();
        properties.getJwt().setSecret(SECRET);
        properties.getJwt().setAccessTokenValidity(Duration.ofHours(1));
        properties.getJwt().setRefreshTokenValidity(Duration.ofDays(7));
        return new JwtTokenProvider(properties, encoder(SECRET), decoder(SECRET));
    }

    private static String signedRefreshToken(String secret, Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(String.valueOf(MEMBER_ID))
                .issuer("TISSUE")
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("tokenType", "refresh")
                .claim("memberId", MEMBER_ID)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder(secret).encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    @Nested
    @DisplayName("create access token")
    class CreateAccessToken {

        @Test
        @DisplayName("success: creating access token returns a valid JWT token string")
        void createAccessToken() {
            // given
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));

            // when
            String token = tokenProvider.createAccessToken(MEMBER_ID, EMAIL, USERNAME, authorities);

            // then
            assertThat(token).isNotBlank();
            assertThat(token.split("\\.")).hasSize(3);
        }
    }

    @Nested
    @DisplayName("validate refresh token and get memberId")
    class ValidateRefreshToken {

        @Test
        @DisplayName("success: a valid refresh token returns the memberId")
        void successRefreshTokenValidation() {
            // given
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
            String token = tokenProvider.createRefreshToken(MEMBER_ID, EMAIL, USERNAME, authorities);

            // when
            Long memberId = tokenProvider.validateRefreshTokenAndGetMemberId(token);

            // then
            assertThat(memberId).isEqualTo(MEMBER_ID);
        }

        @Test
        @DisplayName("fail: using an access token for refresh token validation throws JwtTokenException")
        void failRefreshTokenValidation_If_UseAccessToken() {
            // given
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
            String accessToken = tokenProvider.createAccessToken(MEMBER_ID, EMAIL, USERNAME, authorities);

            // when & then
            assertThatThrownBy(() -> tokenProvider.validateRefreshTokenAndGetMemberId(accessToken))
                    .isInstanceOf(JwtTokenException.class);
        }

        @Test
        @DisplayName("fail: if refresh token is expired, validation throws TokenExpiredException")
        void failRefreshTokenValidation_If_ExpiredToken() {
            // given
            Instant now = Instant.now();
            String expiredToken = signedRefreshToken(SECRET, now.minusSeconds(7200), now.minusSeconds(3600));

            // when & then
            assertThatThrownBy(() -> tokenProvider.validateRefreshTokenAndGetMemberId(expiredToken))
                    .isInstanceOf(TokenExpiredException.class);
        }

        @Test
        @DisplayName("fail: if token signature is invalid, validation throws JwtTokenException")
        void failRefreshTokenValidation_If_InvalidSignature() {
            // given
            Instant now = Instant.now();
            String tamperedToken = signedRefreshToken("x".repeat(32), now, now.plusSeconds(3600));

            // when & then
            assertThatThrownBy(() -> tokenProvider.validateRefreshTokenAndGetMemberId(tamperedToken))
                    .isInstanceOf(JwtTokenException.class);
        }
    }

    @Test
    @DisplayName("fail: if secret is under 32 characters, JwtTokenProvider constructor throws IllegalStateException")
    void failShortSecret() {
        // given
        TissueSecurityProperties properties = new TissueSecurityProperties();
        String shortSecret = "a".repeat(31);
        properties.getJwt().setSecret(shortSecret);

        // when & then
        assertThatThrownBy(() -> new JwtTokenProvider(properties, encoder(SECRET), decoder(SECRET)))
                .isInstanceOf(IllegalStateException.class);
    }
}
