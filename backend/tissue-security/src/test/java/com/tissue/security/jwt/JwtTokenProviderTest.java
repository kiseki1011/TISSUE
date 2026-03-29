package com.tissue.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.security.config.TissueSecurityProperties;
import com.tissue.security.domain.TokenClaims;
import com.tissue.security.domain.exception.TokenExpiredException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class JwtTokenProviderTest {

    private static final String SECRET = "a".repeat(32);
    private static final Long MEMBER_ID = 1L;
    private static final String EMAIL = "user@test.com";
    private static final String USERNAME = "testuser";

    private final JwtTokenProvider tokenProvider = createTokenProvider();

    private static JwtTokenProvider createTokenProvider() {
        TissueSecurityProperties properties = new TissueSecurityProperties();
        properties.getJwt().setSecret(SECRET);
        properties.getJwt().setAccessTokenValidity(Duration.ofHours(1));
        properties.getJwt().setRefreshTokenValidity(Duration.ofDays(7));
        properties.getJwt().setElevatedTokenValidity(Duration.ofMinutes(10));
        return new JwtTokenProvider(properties);
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
            SecretKey secretKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
            String expiredToken = Jwts.builder()
                    .subject(String.valueOf(MEMBER_ID))
                    .issuedAt(Date.from(java.time.Instant.now().minusSeconds(3600)))
                    .expiration(Date.from(java.time.Instant.now().minusSeconds(1)))
                    .issuer("TISSUE")
                    .claim("tokenType", "refresh")
                    .claim("memberId", MEMBER_ID)
                    .signWith(secretKey)
                    .compact();

            // when & then
            assertThatThrownBy(() -> tokenProvider.validateRefreshTokenAndGetMemberId(expiredToken))
                    .isInstanceOf(TokenExpiredException.class);
        }

        @Test
        @DisplayName("fail: if token signature is invalid, validation throws JwtTokenException")
        void failRefreshTokenValidation_If_InvalidSignature() {
            // given
            String wrongSecret = "x".repeat(32);
            SecretKey wrongKey = Keys.hmacShaKeyFor(wrongSecret.getBytes(StandardCharsets.UTF_8));

            String tamperedToken = Jwts.builder()
                    .subject(String.valueOf(MEMBER_ID))
                    .issuedAt(Date.from(java.time.Instant.now()))
                    .expiration(Date.from(java.time.Instant.now().plusSeconds(3600)))
                    .issuer("TISSUE")
                    .claim("tokenType", "refresh")
                    .claim("memberId", MEMBER_ID)
                    .signWith(wrongKey)
                    .compact();

            // when & then
            assertThatThrownBy(() -> tokenProvider.validateRefreshTokenAndGetMemberId(tamperedToken))
                    .isInstanceOf(JwtTokenException.class);
        }
    }

    @Nested
    @DisplayName("validate register token")
    class ValidateRegisterToken {

        @Test
        @DisplayName("success: valid register token returns TokenClaims with provider information")
        void successRegisterTokenValidation() {
            // given
            String token = tokenProvider.createRegisterToken("github", "12345", EMAIL);

            // when
            TokenClaims claims = tokenProvider.validateRegisterToken(token);

            // then
            assertThat(claims.provider()).isEqualTo("github");
            assertThat(claims.identifier()).isEqualTo("12345");
            assertThat(claims.email()).isEqualTo(EMAIL);
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
        assertThatThrownBy(() -> new JwtTokenProvider(properties)).isInstanceOf(IllegalStateException.class);
    }
}
