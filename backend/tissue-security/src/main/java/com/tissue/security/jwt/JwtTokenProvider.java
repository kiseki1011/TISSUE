package com.tissue.security.jwt;

import com.tissue.security.config.SecurityProperties;
import com.tissue.security.domain.TokenClaims;
import com.tissue.security.domain.TokenProvider;
import com.tissue.security.domain.TokenType;
import com.tissue.security.domain.exception.TokenExpiredException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JwtTokenProvider implements TokenProvider {

    public static final String ISSUER = "TISSUE";
    public static final int SECRET_KEY_LENGTH = 32;

    private final SecretKey secretKey;
    private final Duration accessTokenValidity;
    private final Duration refreshTokenValidity;
    private final Duration elevatedTokenValidity;
    private final Duration registerTokenValidity = Duration.ofMinutes(10);

    public JwtTokenProvider(SecurityProperties securityProperties) {
        SecurityProperties.Jwt jwt = securityProperties.getJwt();
        String secret = jwt.getSecret();

        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < SECRET_KEY_LENGTH) {
            throw new IllegalStateException("JWT secret must be at least " + SECRET_KEY_LENGTH + " bytes (UTF-8).");
        }

        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidity = jwt.getAccessTokenValidity();
        this.refreshTokenValidity = jwt.getRefreshTokenValidity();
        this.elevatedTokenValidity = jwt.getElevatedTokenValidity();
    }

    @Override
    public String createAccessToken(
            Long memberId,
            @Nullable String email,
            String username,
            Collection<? extends GrantedAuthority> authorities) {
        return createToken(memberId, TokenType.ACCESS, accessTokenValidity, false, email, username, authorities);
    }

    @Override
    public String createRefreshToken(
            Long memberId,
            @Nullable String email,
            String username,
            Collection<? extends GrantedAuthority> authorities) {
        return createToken(memberId, TokenType.REFRESH, refreshTokenValidity, false, email, username, authorities);
    }

    @Override
    public String createElevatedToken(
            Long memberId,
            @Nullable String email,
            String username,
            Collection<? extends GrantedAuthority> authorities) {
        return createToken(memberId, TokenType.ACCESS, elevatedTokenValidity, true, email, username, authorities);
    }

    // TODO: consider separating from TokenProvider
    @Override
    public String createRegisterToken(String provider, String identifier, String email) {
        try {
            Instant now = Instant.now();
            return Jwts.builder()
                    .subject(email)
                    .issuedAt(Date.from(now))
                    .expiration(Date.from(now.plus(registerTokenValidity)))
                    .issuer(ISSUER)
                    .claim(CLAIM_TOKEN_TYPE, TokenType.REGISTER.getValue())
                    .claim(CLAIM_PROVIDER, provider)
                    .claim(CLAIM_IDENTIFIER, identifier)
                    .claim(CLAIM_EMAIL, email)
                    .signWith(secretKey)
                    .compact();

        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtTokenException("Failed to create REGISTER token", e);
        }
    }

    @Override
    public TokenClaims validateRegisterToken(String token) {
        Claims claims = parseAndValidateClaims(token);
        validateTokenType(claims, TokenType.REGISTER);

        return TokenClaims.builder()
                .subject(claims.getSubject())
                .provider(claims.get(CLAIM_PROVIDER, String.class))
                .identifier(claims.get(CLAIM_IDENTIFIER, String.class))
                .email(claims.get(CLAIM_EMAIL, String.class))
                .build();
    }

    @Override
    public Duration getRefreshTokenValidity() {
        return refreshTokenValidity;
    }

    @Override
    public Long validateRefreshTokenAndGetMemberId(String token) {
        Claims claims = parseAndValidateClaims(token);
        validateTokenType(claims, TokenType.REFRESH);
        return claims.get(CLAIM_MEMBER_ID, Long.class);
    }

    private String createToken(
            Long memberId,
            TokenType tokenType,
            Duration validity,
            boolean isElevated,
            @Nullable String email,
            String username,
            Collection<? extends GrantedAuthority> authorities) {
        try {
            Instant now = Instant.now();
            List<String> roles =
                    authorities.stream().map(GrantedAuthority::getAuthority).toList();

            JwtBuilder builder = Jwts.builder()
                    .subject(String.valueOf(memberId))
                    .issuedAt(Date.from(now))
                    .expiration(Date.from(now.plus(validity)))
                    .issuer(ISSUER)
                    .claim(CLAIM_TOKEN_TYPE, tokenType.getValue())
                    .claim(CLAIM_MEMBER_ID, memberId)
                    .claim(CLAIM_EMAIL, email)
                    .claim(CLAIM_USERNAME, username)
                    .claim(CLAIM_ELEVATED, isElevated)
                    .claim(CLAIM_AUTHORITIES, roles)
                    .signWith(secretKey);

            if (Objects.equals(TokenType.REFRESH, tokenType)) {
                builder.claim(CLAIM_JTI, UUID.randomUUID().toString());
            }
            return builder.compact();

        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtTokenException();
        }
    }

    private void validateTokenType(Claims claims, TokenType expectedType) {
        TokenType tokenType = TokenType.from(claims.get(CLAIM_TOKEN_TYPE, String.class));
        if (!Objects.equals(expectedType, tokenType)) {
            throw new JwtTokenException();
        }
    }

    private Claims parseAndValidateClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

        } catch (ExpiredJwtException e) {
            throw new TokenExpiredException();
        } catch (JwtException | SecurityException | IllegalArgumentException e) {
            throw new JwtTokenException();
        }
    }
}
