package com.tissue.api.security.authentication.jwt;

import static com.tissue.api.security.util.MaskingUtil.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import com.tissue.api.security.authentication.MemberUserDetails;
import com.tissue.api.security.authentication.MemberUserDetailsService;
import com.tissue.api.security.authentication.TokenType;
import com.tissue.api.security.authentication.exception.JwtAuthenticationException;
import com.tissue.api.security.authentication.exception.JwtCreationException;
import com.tissue.api.security.authentication.exception.JwtSecretException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JwtTokenService {

	// TODO: move constants to a seperate abstract class
	public static final String CLAIM_TOKEN_TYPE = "tokenType";
	public static final String CLAIM_MEMBER_ID = "memberId";
	public static final String CLAIM_ELEVATED = "elevated";
	public static final String CLAIM_JTI = "jti";
	public static final String ISSUER = "tissue-api";
	public static final int SECRET_KEY_LENGTH = 32;

	private final SecretKey secretKey;
	private final long accessTokenValidityInSeconds;
	private final long refreshTokenValidityInSeconds;
	private final long elevatedTokenValidityInSeconds;
	private final MemberUserDetailsService userDetailsService;

	/**
	 * Initialize secret key and validity in constructor
	 */
	// TODO: use properties class
	public JwtTokenService(
		@Value("${jwt.secret}") String secret,
		@Value("${jwt.access-token-validity:3600}") long accessTokenValidityInSeconds,
		@Value("${jwt.refresh-token-validity:604800}") long refreshTokenValidityInSeconds,
		@Value("${jwt.elevated-token-validity:300}") long elevatedTokenValidityInSeconds,
		MemberUserDetailsService userDetailsService
	) {
		this.userDetailsService = userDetailsService;

		// validate secret key length
		if (secret.length() < SECRET_KEY_LENGTH) {
			throw new JwtSecretException(
				"JWT secret must be at least 256 bits (32 characters) long for security. Current length: "
					+ secret.length()
			);
		}

		this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.accessTokenValidityInSeconds = accessTokenValidityInSeconds;
		this.refreshTokenValidityInSeconds = refreshTokenValidityInSeconds;
		this.elevatedTokenValidityInSeconds = elevatedTokenValidityInSeconds;

		log.info("JwtTokenProvider initialized (HS256)");
	}

	/**
	 * Create Access Token
	 * - subject: loginId
	 * - memberId: Primary Key for Member
	 * - tokenType: "access"
	 */
	public String createAccessToken(Long memberId, String loginIdentifier) {
		return createToken(loginIdentifier, TokenType.ACCESS, accessTokenValidityInSeconds, false, memberId);
	}

	/**
	 * Create Refresh Token
	 */
	public String createRefreshToken(Long memberId, String loginIdentifier) {
		return createToken(loginIdentifier, TokenType.REFRESH, refreshTokenValidityInSeconds, false, memberId);
	}

	/**
	 * Create Elevated (Access) Token
	 */
	public String createElevatedToken(Long memberId, String loginIdentifier) {
		return createToken(loginIdentifier, TokenType.ACCESS, elevatedTokenValidityInSeconds, true, memberId);
	}

	private String createToken(
		String subject,
		TokenType tokenType,
		long validitySeconds,
		boolean isElevated,
		Long memberId
	) {
		try {
			Instant now = Instant.now();
			JwtBuilder builder = Jwts.builder()
				.subject(subject) // JWT subject - Member identifier (loginId or email)
				.issuedAt(Date.from(now)) // issued date
				.expiration(Date.from(now.plusSeconds(validitySeconds))) // expiration date
				.issuer(ISSUER) // issuer information
				.claim(CLAIM_TOKEN_TYPE, tokenType.getValue())
				.claim(CLAIM_MEMBER_ID, memberId)
				.claim(CLAIM_ELEVATED, isElevated)
				.signWith(secretKey);

			if (Objects.equals(TokenType.REFRESH, tokenType)) {
				builder.claim(CLAIM_JTI, UUID.randomUUID().toString()); // JWT ID - for tracking token
			}

			return builder.compact(); // compact into a token string

		} catch (JwtException | IllegalArgumentException e) {
			log.error("Failed to create {} token. subject: {}", tokenType, maskIdentifier(subject));
			throw new JwtCreationException("Failed to create " + tokenType + " token.", e);
		}
	}

	/**
	 * Create Authentication from the JWT token
	 *
	 * 1. Validate token (format, sign, expiration time)
	 * 2. Extract subject(member identifier) from token
	 * 3. Get MemberUserDetails using userDetailsService.loadUserByUsername
	 * 4. Create and return Authentication object
	 */
	public Authentication getAuthentication(String token) {

		String loginIdentifier = null;

		try {
			// Validate token
			validateAccessToken(token);

			// Extract subject(member identifier)
			loginIdentifier = getSubjectFromToken(token);

			// Get MemberUserDetails(check real time status of the member)
			MemberUserDetails userDetails = (MemberUserDetails)userDetailsService.loadUserByUsername(loginIdentifier);

			boolean elevated = getElevatedFromToken(token);
			userDetails.setElevated(elevated);

			// Create Authentication object
			//  - principal: user's information (UserDetails)
			//  - credentials: null (Does not need a password since JWT token is the authentication medium)
			//  - authorities: list of the authorities of the user
			return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

		} catch (UsernameNotFoundException e) {
			log.warn("Member not found for identifier: {}", maskIdentifier(loginIdentifier));
			throw new JwtAuthenticationException("Member not found.", e);
		} catch (JwtException e) {
			log.warn("JWT validation failed. token: {}", maskToken(token));
			throw new JwtAuthenticationException("JWT validation failed.", e);
		}
	}

	// TODO: Using parseAndValidateClaims(token) is redundant. Might be better to pass claims as the parameter.

	/**
	 * Extract subject(identifier) from token
	 */
	public String getSubjectFromToken(String token) {
		return parseAndValidateClaims(token).getSubject();
	}

	/**
	 * Extract elevated claim from token
	 */
	public boolean getElevatedFromToken(String token) {
		Boolean elevated = parseAndValidateClaims(token).get(CLAIM_ELEVATED, Boolean.class);
		return elevated != null && elevated;
	}

	/**
	 * Extract memberId from token
	 */
	public Long getMemberIdFromToken(String token) {

		Object memberIdClaim = parseAndValidateClaims(token).get(CLAIM_MEMBER_ID);

		if (memberIdClaim instanceof Number number) {
			return number.longValue();
		}

		log.warn("Invalid or missing memberId claim. memberIdClaim type: {}", memberIdClaim.getClass());
		throw new JwtAuthenticationException("Invalid or missing memberId claim.");
	}

	/**
	 * Check token type (access/refresh)
	 */
	public String getTokenType(String token) {
		return parseAndValidateClaims(token).get(CLAIM_TOKEN_TYPE, String.class);
	}

	/**
	 * Validate Access Token
	 */
	public void validateAccessToken(String token) {
		Claims claims = parseAndValidateClaims(token);
		validateTokenType(claims, TokenType.ACCESS);
		validateRequiredClaims(claims);
	}

	/**
	 * Validate Refresh Token
	 */
	public void validateRefreshToken(String token) {
		Claims claims = parseAndValidateClaims(token);
		validateTokenType(claims, TokenType.REFRESH);
		validateRequiredClaims(claims);
	}

	private void validateTokenType(Claims claims, TokenType expectedType) {
		TokenType tokenType = TokenType.from(claims.get(CLAIM_TOKEN_TYPE, String.class));
		if (!Objects.equals(expectedType, tokenType)) {
			throw new JwtAuthenticationException(
				"Token validation failed. Expected type: " + expectedType + ", actual: " + tokenType);
		}
	}

	private void validateRequiredClaims(Claims claims) {
		if (claims.getSubject() == null) {
			throw new JwtAuthenticationException("Token validation failed. Subject claim is missing.");
		}
		if (claims.get(CLAIM_MEMBER_ID) == null) {
			throw new JwtAuthenticationException("Token validation failed. Member ID claim is missing.");
		}
	}

	/**
	 * Calculate the expiration time of a token (seconds)
	 * Can be used by client when refreshing token.
	 */
	// TODO: Delete if not used.
	public long getTokenRemainingSeconds(String token) {
		try {
			Claims claims = parseAndValidateClaims(token);
			Date expiration = claims.getExpiration();

			long remainingMillis = expiration.getTime() - System.currentTimeMillis();
			return Math.max(0, remainingMillis / 1000);

		} catch (JwtException e) {
			log.debug("Failed to calculate remaining time for token.", e);
			return 0;
		}
	}

	/**
	 * Parse and validate token
	 */
	private Claims parseAndValidateClaims(String token) {
		try {
			return Jwts.parser()
				.verifyWith(secretKey)
				.build()
				.parseSignedClaims(token)
				.getPayload();

		} catch (ExpiredJwtException e) {
			log.warn("Token is expired. token: {}", maskToken(token));
			throw new JwtAuthenticationException("Token is expired.", e);
		} catch (UnsupportedJwtException e) {
			log.warn("Unsupported JWT token. token: {}", maskToken(token));
			throw new JwtAuthenticationException("Unsupported JWT token.", e);
		} catch (MalformedJwtException e) {
			log.warn("Malformed JWT token. token: {}", maskToken(token));
			throw new JwtAuthenticationException("Malformed JWT token.", e);
		} catch (SecurityException | IllegalArgumentException e) {
			log.warn("Invalid JWT token. token: {}", maskToken(token));
			throw new JwtAuthenticationException("Invalid JWT token.", e);
		}
	}
}
