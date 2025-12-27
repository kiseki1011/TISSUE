package com.tissue.security.authentication.jwt;

import com.tissue.security.authentication.MemberUserDetails;
import com.tissue.security.authentication.MemberUserDetailsService;
import com.tissue.security.authentication.TokenType;
import com.tissue.security.authentication.exception.JwtAuthenticationException;
import com.tissue.security.authentication.exception.JwtCreationException;
import com.tissue.security.authentication.exception.JwtSecretException;
import com.tissue.security.util.MaskingUtil;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JwtTokenService {

    // TODO: should i move constants to a separate interface
    public static final String CLAIM_TOKEN_TYPE = "tokenType";
    public static final String CLAIM_MEMBER_ID = "memberId";
    public static final String CLAIM_ELEVATED = "elevated";
    public static final String CLAIM_JTI = "jti";
    public static final String ISSUER = "tissue";
    public static final int SECRET_KEY_LENGTH = 32;

    private final SecretKey secretKey;
    private final long accessTokenValidityInSeconds;
    private final long refreshTokenValidityInSeconds;
    private final long elevatedTokenValidityInSeconds;
    private final MemberUserDetailsService userDetailsService;

    /** Initialize secret key and validity in constructor */
    // TODO: should i use a properties class
    public JwtTokenService(
            @Value("${tissue.jwt.secret}") String secret,
            @Value("${tissue.jwt.access-token-validity:3600}") long accessTokenValidityInSeconds,
            @Value("${tissue.jwt.refresh-token-validity:604800}")
                    long refreshTokenValidityInSeconds,
            @Value("${tissue.jwt.elevated-token-validity:300}") long elevatedTokenValidityInSeconds,
            MemberUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;

        if (secret.length() < SECRET_KEY_LENGTH) {
            throw new JwtSecretException(
                    ("JWT secret must be at least 256 bits (32 characters) long for security. "
                                    + "Current length: %d")
                            .formatted(secret.length()));
        }

        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidityInSeconds = accessTokenValidityInSeconds;
        this.refreshTokenValidityInSeconds = refreshTokenValidityInSeconds;
        this.elevatedTokenValidityInSeconds = elevatedTokenValidityInSeconds;

        log.info("JwtTokenProvider initialized (HS256)");
    }

    /**
     * Create Access Token - subject: email - memberId: Primary Key for Member - tokenType: "access"
     */
    public String createAccessToken(Long memberId, String email) {
        return createToken(email, TokenType.ACCESS, accessTokenValidityInSeconds, false, memberId);
    }

    /** Create Refresh Token */
    public String createRefreshToken(Long memberId, String email) {
        return createToken(
                email, TokenType.REFRESH, refreshTokenValidityInSeconds, false, memberId);
    }

    /** Create Elevated (Access) Token */
    public String createElevatedToken(Long memberId, String email) {
        return createToken(email, TokenType.ACCESS, elevatedTokenValidityInSeconds, true, memberId);
    }

    private String createToken(
            String subject,
            TokenType tokenType,
            long validitySeconds,
            boolean isElevated,
            Long memberId) {
        try {
            Instant now = Instant.now();
            JwtBuilder builder =
                    Jwts.builder()
                            .subject(subject) // JWT subject - Member identifier (email)
                            .issuedAt(Date.from(now)) // issued date
                            .expiration(
                                    Date.from(now.plusSeconds(validitySeconds))) // expiration date
                            .issuer(ISSUER) // issuer information
                            .claim(CLAIM_TOKEN_TYPE, tokenType.getValue())
                            .claim(CLAIM_MEMBER_ID, memberId)
                            .claim(CLAIM_ELEVATED, isElevated)
                            .signWith(secretKey);

            if (Objects.equals(TokenType.REFRESH, tokenType)) {
                builder.claim(
                        CLAIM_JTI, UUID.randomUUID().toString()); // JWT ID - for tracking token
            }

            return builder.compact(); // compact into a token string

        } catch (JwtException | IllegalArgumentException e) {
            log.error(
                    "Failed to create {} token. subject: {}",
                    tokenType,
                    MaskingUtil.maskIdentifier(subject));
            throw new JwtCreationException(
                    "Failed to create %s token".formatted(tokenType.getValue()), e);
        }
    }

    /**
     * Create Authentication from the JWT token
     *
     * <p>1. Validate token (format, sign, expiration time) 2. Extract subject(member identifier)
     * from token 3. Get MemberUserDetails using userDetailsService.loadUserByUsername 4. Create and
     * return Authentication object
     */
    public Authentication getAuthentication(String token) {
        String email = null;

        try {
            validateAccessToken(token);
            email = getSubjectFromToken(token);

            // Get MemberUserDetails(check real time status of the member)
            MemberUserDetails userDetails =
                    (MemberUserDetails) userDetailsService.loadUserByUsername(email);

            boolean elevated = getElevatedFromToken(token);
            userDetails.setElevated(elevated);

            // Create Authentication object
            //  - principal: user's information (UserDetails)
            //  - credentials: null (Does not need a password since JWT token is the authentication
            // medium)
            //  - authorities: list of the authorities of the user
            return new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());

        } catch (UsernameNotFoundException e) {
            // TODO: do i really need to mask the email?
            log.warn("Member not found for email: {}", MaskingUtil.maskIdentifier(email));
            throw new JwtAuthenticationException(
                    "Member not found for email: %s".formatted(email), e);
        } catch (JwtException e) {
            log.warn("JWT validation failed. token: {}", MaskingUtil.maskToken(token));
            throw new JwtAuthenticationException("JWT validation failed", e);
        }
    }

    /** Extract subject(identifier) from token */
    public String getSubjectFromToken(String token) {
        return parseAndValidateClaims(token).getSubject();
    }

    /** Extract elevated claim from token */
    public boolean getElevatedFromToken(String token) {
        Boolean elevated = parseAndValidateClaims(token).get(CLAIM_ELEVATED, Boolean.class);
        return elevated != null && elevated;
    }

    /** Extract memberId from token */
    public Long getMemberIdFromToken(String token) {
        Object memberIdClaim = parseAndValidateClaims(token).get(CLAIM_MEMBER_ID);

        if (memberIdClaim instanceof Number number) {
            return number.longValue();
        }

        log.warn(
                "Invalid or missing member ID claim. member ID claim type: {}",
                memberIdClaim.getClass());
        throw new JwtAuthenticationException("Invalid or missing member ID claim");
    }

    /** Check token type (access/refresh) */
    public String getTokenType(String token) {
        return parseAndValidateClaims(token).get(CLAIM_TOKEN_TYPE, String.class);
    }

    /** Validate Access Token */
    public void validateAccessToken(String token) {
        Claims claims = parseAndValidateClaims(token);
        validateTokenType(claims, TokenType.ACCESS);
        validateRequiredClaims(claims);
    }

    /** Validate Refresh Token */
    public void validateRefreshToken(String token) {
        Claims claims = parseAndValidateClaims(token);
        validateTokenType(claims, TokenType.REFRESH);
        validateRequiredClaims(claims);
    }

    private void validateTokenType(Claims claims, TokenType expectedType) {
        TokenType tokenType = TokenType.from(claims.get(CLAIM_TOKEN_TYPE, String.class));
        if (!Objects.equals(expectedType, tokenType)) {
            throw new JwtAuthenticationException(
                    "Token validation failed. Expected type: %s | Actual: %s"
                            .formatted(expectedType.getValue(), tokenType.getValue()));
        }
    }

    private void validateRequiredClaims(Claims claims) {
        if (claims.getSubject() == null) {
            throw new JwtAuthenticationException(
                    "Token validation failed. Subject claim is missing.");
        }
        if (claims.get(CLAIM_MEMBER_ID) == null) {
            throw new JwtAuthenticationException(
                    "Token validation failed. Member ID claim is missing.");
        }
    }

    /**
     * Calculate the expiration time of a token (seconds) Can be used by client when refreshing
     * token.
     */
    public long getTokenRemainingSeconds(String token) {
        try {
            Claims claims = parseAndValidateClaims(token);
            Date expiration = claims.getExpiration();

            long remainingMillis = expiration.getTime() - System.currentTimeMillis();
            return Math.max(0, remainingMillis / 1000);

        } catch (JwtException e) {
            log.debug("Failed to calculate remaining time for token", e);
            return 0;
        }
    }

    /** Parse and validate token */
    private Claims parseAndValidateClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

        } catch (ExpiredJwtException e) {
            log.warn("Token is expired. token: {}", MaskingUtil.maskToken(token));
            throw new JwtAuthenticationException("Token is expired", e);
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token. token: {}", MaskingUtil.maskToken(token));
            throw new JwtAuthenticationException("Unsupported JWT token", e);
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token. token: {}", MaskingUtil.maskToken(token));
            throw new JwtAuthenticationException("Malformed JWT token", e);
        } catch (SecurityException | IllegalArgumentException e) {
            log.warn("Invalid JWT token. token: {}", MaskingUtil.maskToken(token));
            throw new JwtAuthenticationException("Invalid JWT token", e);
        }
    }
}
