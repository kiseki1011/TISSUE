package com.tissue.jwt;

import com.tissue.domain.TokenClaims;
import com.tissue.domain.TokenProvider;
import com.tissue.domain.TokenType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Slf4j
@Getter
@Component
public class JwtTokenProvider implements TokenProvider {

    public static final String ISSUER = "tissue";
    public static final int SECRET_KEY_LENGTH = 32;

    private final SecretKey secretKey;
    private final long accessTokenValidityInSeconds;
    private final long refreshTokenValidityInSeconds;
    private final long elevatedTokenValidityInSeconds;
    private final long registerTokenValidityInSeconds = 600;

    public JwtTokenProvider(
            @Value("${tissue.security.jwt.secret}") String secret,
            @Value("${tissue.security.jwt.access-token-validity:3600}") long accessTokenValidityInSeconds,
            @Value("${tissue.security.jwt.refresh-token-validity:604800}") long refreshTokenValidityInSeconds,
            @Value("${tissue.security.jwt.elevated-token-validity:300}") long elevatedTokenValidityInSeconds) {

        if (secret.length() < SECRET_KEY_LENGTH) {
            throw new IllegalStateException("JWT secret is too short.");
        }

        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidityInSeconds = accessTokenValidityInSeconds;
        this.refreshTokenValidityInSeconds = refreshTokenValidityInSeconds;
        this.elevatedTokenValidityInSeconds = elevatedTokenValidityInSeconds;
    }

    @Override
    public String createAccessToken(
            Long memberId, String email, String username, Collection<? extends GrantedAuthority> authorities) {
        return createToken(
                email, TokenType.ACCESS, accessTokenValidityInSeconds, false, memberId, username, authorities);
    }

    @Override
    public String createRefreshToken(
            Long memberId, String email, String username, Collection<? extends GrantedAuthority> authorities) {
        return createToken(
                email, TokenType.REFRESH, refreshTokenValidityInSeconds, false, memberId, username, authorities);
    }

    @Override
    public String createElevatedToken(
            Long memberId, String email, String username, Collection<? extends GrantedAuthority> authorities) {
        return createToken(
                email, TokenType.ACCESS, elevatedTokenValidityInSeconds, true, memberId, username, authorities);
    }

    @Override
    public String createRegisterToken(String provider, String identifier, String email) {
        try {
            Instant now = Instant.now();
            return Jwts.builder()
                    .subject(email)
                    .issuedAt(Date.from(now))
                    .expiration(Date.from(now.plusSeconds(registerTokenValidityInSeconds)))
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
    public String getSubjectFromToken(String token) {
        return parseAndValidateClaims(token).getSubject();
    }

    @Override
    public void validateRefreshToken(String token) {
        Claims claims = parseAndValidateClaims(token);
        validateTokenType(claims, TokenType.REFRESH);
    }

    private String createToken(
            String subject,
            TokenType tokenType,
            long validitySeconds,
            boolean isElevated,
            Long memberId,
            String name,
            Collection<? extends GrantedAuthority> authorities) {
        try {
            Instant now = Instant.now();
            List<String> roles =
                    authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList());

            JwtBuilder builder = Jwts.builder()
                    .subject(subject)
                    .issuedAt(Date.from(now))
                    .expiration(Date.from(now.plusSeconds(validitySeconds)))
                    .issuer(ISSUER)
                    .claim(CLAIM_TOKEN_TYPE, tokenType.getValue())
                    .claim(CLAIM_MEMBER_ID, memberId)
                    .claim(CLAIM_USERNAME, name)
                    .claim(CLAIM_ELEVATED, isElevated)
                    .claim(CLAIM_AUTHORITIES, roles)
                    .signWith(secretKey);

            if (Objects.equals(TokenType.REFRESH, tokenType)) {
                builder.claim(CLAIM_JTI, UUID.randomUUID().toString());
            }
            return builder.compact();

        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtTokenException("Token creation failed", e);
        }
    }

    private void validateTokenType(Claims claims, TokenType expectedType) {
        TokenType tokenType = TokenType.from(claims.get(CLAIM_TOKEN_TYPE, String.class));
        if (!Objects.equals(expectedType, tokenType)) {
            throw new JwtTokenException("Token type mismatch");
        }
    }

    private Claims parseAndValidateClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

        } catch (JwtException | SecurityException | IllegalArgumentException e) {
            throw new JwtTokenException("JWT validation failed", e);
        }
    }
}
