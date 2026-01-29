package com.tissue.global.security.jwt;

import com.tissue.authentication.application.port.out.TokenProvider;
import com.tissue.authentication.domain.TokenType;
import com.tissue.global.security.exception.ExpiredTokenException;
import com.tissue.global.security.exception.InvalidTokenException;
import com.tissue.global.security.exception.JwtCreationException;
import com.tissue.global.security.exception.JwtSecretException;
import com.tissue.global.security.exception.MalformedTokenException;
import com.tissue.global.security.exception.TokenMissingClaimException;
import com.tissue.global.security.exception.UnsupportedTokenException;
import com.tissue.global.security.principal.MemberDetails;
import com.tissue.global.security.principal.MemberDetailsService;
import com.tissue.global.security.util.MaskingUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
    private final long registerTokenValidityInSeconds = 600; // 10 minutes

    private final MemberDetailsService userDetailsService;

    // TODO: Consider using a properties class
    public JwtTokenProvider(
            @Value("${tissue.security.jwt.secret}") String secret,
            @Value("${tissue.security.jwt.access-token-validity:3600}") long accessTokenValidityInSeconds,
            @Value("${tissue.security.jwt.refresh-token-validity:604800}") long refreshTokenValidityInSeconds,
            @Value("${tissue.security.jwt.elevated-token-validity:300}") long elevatedTokenValidityInSeconds,
            MemberDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;

        if (secret.length() < SECRET_KEY_LENGTH) {
            throw new JwtSecretException(
                    ("JWT secret must be at least 256 bits (32 characters) long for security. " + "Current length: %d")
                            .formatted(secret.length()));
        }

        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidityInSeconds = accessTokenValidityInSeconds;
        this.refreshTokenValidityInSeconds = refreshTokenValidityInSeconds;
        this.elevatedTokenValidityInSeconds = elevatedTokenValidityInSeconds;

        log.info("JwtTokenProvider initialized (HS256)");
    }

    @Override
    public String createAccessToken(Long memberId, String email) {
        return createToken(email, TokenType.ACCESS, accessTokenValidityInSeconds, false, memberId);
    }

    @Override
    public String createRefreshToken(Long memberId, String email) {
        return createToken(email, TokenType.REFRESH, refreshTokenValidityInSeconds, false, memberId);
    }

    @Override
    public String createElevatedToken(Long memberId, String email) {
        return createToken(email, TokenType.ACCESS, elevatedTokenValidityInSeconds, true, memberId);
    }

    /**
     * Create Register Token for OAuth2 Signup.
     */
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
            throw new JwtCreationException("Failed to create REGISTER token", e);
        }
    }

    @Override
    public Claims validateRegisterToken(String token) {
        Claims claims = parseAndValidateClaims(token);
        validateTokenType(claims, TokenType.REGISTER);
        if (claims.get(CLAIM_PROVIDER) == null || claims.get(CLAIM_IDENTIFIER) == null) {
            throw new TokenMissingClaimException("Token validation failed. Required claims missing.");
        }
        return claims;
    }

    /**
     * Create Authentication object from the JWT token.
     */
    @Override
    public Authentication getAuthentication(String token) {
        String email = null;

        try {
            validateAccessToken(token);
            email = getSubjectFromToken(token);

            MemberDetails userDetails = (MemberDetails) userDetailsService.loadUserByUsername(email);

            boolean elevated = getElevatedFromToken(token);
            userDetails.setElevated(elevated);

            return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        } catch (UsernameNotFoundException e) {
            log.warn("Member not found for email: {}", MaskingUtil.maskIdentifier(email));
            throw new InvalidTokenException("Member not found for email: %s".formatted(email), e);
        } catch (JwtException e) {
            log.warn("JWT validation failed. token: {}", MaskingUtil.maskToken(token));
            throw new InvalidTokenException("JWT validation failed", e);
        }
    }

    @Override
    public String getSubjectFromToken(String token) {
        return parseAndValidateClaims(token).getSubject();
    }

    @Override
    public boolean getElevatedFromToken(String token) {
        Boolean elevated = parseAndValidateClaims(token).get(CLAIM_ELEVATED, Boolean.class);
        return elevated != null && elevated;
    }

    @Override
    public void validateAccessToken(String token) {
        Claims claims = parseAndValidateClaims(token);
        validateTokenType(claims, TokenType.ACCESS);
        validateRequiredClaims(claims);
    }

    @Override
    public void validateRefreshToken(String token) {
        Claims claims = parseAndValidateClaims(token);
        validateTokenType(claims, TokenType.REFRESH);
        validateRequiredClaims(claims);
    }

    private String createToken(
            String subject, TokenType tokenType, long validitySeconds, boolean isElevated, Long memberId) {
        try {
            Instant now = Instant.now();
            JwtBuilder builder = Jwts.builder()
                    .subject(subject) // JWT subject - email
                    .issuedAt(Date.from(now))
                    .expiration(Date.from(now.plusSeconds(validitySeconds)))
                    .issuer(ISSUER)
                    .claim(CLAIM_TOKEN_TYPE, tokenType.getValue())
                    .claim(CLAIM_MEMBER_ID, memberId)
                    .claim(CLAIM_ELEVATED, isElevated)
                    .signWith(secretKey);

            if (Objects.equals(TokenType.REFRESH, tokenType)) {
                builder.claim(CLAIM_JTI, UUID.randomUUID().toString()); // JWT ID - for tracking token
            }

            return builder.compact();

        } catch (JwtException | IllegalArgumentException e) {
            log.error("Failed to create {} token. subject: {}", tokenType, MaskingUtil.maskIdentifier(subject));
            throw new JwtCreationException("Failed to create %s token".formatted(tokenType.getValue()), e);
        }
    }

    private void validateTokenType(Claims claims, TokenType expectedType) {
        TokenType tokenType = TokenType.from(claims.get(CLAIM_TOKEN_TYPE, String.class));
        if (!Objects.equals(expectedType, tokenType)) {
            throw new InvalidTokenException("Token validation failed. Expected type: %s | Actual: %s"
                    .formatted(expectedType.getValue(), tokenType.getValue()));
        }
    }

    private void validateRequiredClaims(Claims claims) {
        if (claims.getSubject() == null) {
            throw new TokenMissingClaimException("Token validation failed. Subject claim is missing.");
        }
        if (claims.get(CLAIM_MEMBER_ID) == null) {
            throw new TokenMissingClaimException("Token validation failed. Member ID claim is missing.");
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
            log.warn("Token is expired. token: {}", MaskingUtil.maskToken(token));
            throw new ExpiredTokenException("Token is expired", e);
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token. token: {}", MaskingUtil.maskToken(token));
            throw new UnsupportedTokenException("Unsupported JWT token", e);
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token. token: {}", MaskingUtil.maskToken(token));
            throw new MalformedTokenException("Malformed JWT token", e);
        } catch (SecurityException | IllegalArgumentException e) {
            log.warn("Invalid JWT token. token: {}", MaskingUtil.maskToken(token));
            throw new InvalidTokenException("Invalid JWT token", e);
        }
    }
}
