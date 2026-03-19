package com.tissue.security.jwt;

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

    public static final String ISSUER = "TISSUE";
    public static final int SECRET_KEY_LENGTH = 32;

    private final SecretKey secretKey;
    private final Duration accessTokenValidity;
    private final Duration refreshTokenValidity;
    private final Duration elevatedTokenValidity;
    private final Duration registerTokenValidity = Duration.ofMinutes(10);

    public JwtTokenProvider(
            @Value("${tissue.security.jwt.secret}") String secret,
            @Value("${tissue.security.jwt.access-token-validity:1h}") Duration accessTokenValidity,
            @Value("${tissue.security.jwt.refresh-token-validity:7d}") Duration refreshTokenValidity,
            @Value("${tissue.security.jwt.elevated-token-validity:10m}") Duration elevatedTokenValidity) {

        if (secret.length() < SECRET_KEY_LENGTH) {
            throw new IllegalStateException("JWT secret is too short.");
        }

        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidity = accessTokenValidity;
        this.refreshTokenValidity = refreshTokenValidity;
        this.elevatedTokenValidity = elevatedTokenValidity;
    }

    @Override
    public String createAccessToken(
            Long memberId, String email, String username, Collection<? extends GrantedAuthority> authorities) {
        return createToken(email, TokenType.ACCESS, accessTokenValidity, false, memberId, username, authorities);
    }

    @Override
    public String createRefreshToken(
            Long memberId, String email, String username, Collection<? extends GrantedAuthority> authorities) {
        return createToken(email, TokenType.REFRESH, refreshTokenValidity, false, memberId, username, authorities);
    }

    @Override
    public String createElevatedToken(
            Long memberId, String email, String username, Collection<? extends GrantedAuthority> authorities) {
        return createToken(email, TokenType.ACCESS, elevatedTokenValidity, true, memberId, username, authorities);
    }

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
            Duration validity,
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
                    .expiration(Date.from(now.plus(validity)))
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
