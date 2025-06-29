package com.tissue.api.security.authentication.jwt;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.tissue.api.security.authentication.exception.JwtAuthenticationException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT 토큰 생성, 검증, 파싱
 * 1. 로그인 성공시 JWT 토큰 생성
 * 2. 매 요청마다 토큰 유효성 검증
 * 3. 토큰에서 사용자 정보 추출
 */
@Slf4j
@Component
public class JwtTokenProvider {

	private final SecretKey secretKey;
	private final long accessTokenValidityInSeconds;
	private final long refreshTokenValidityInSeconds;

	/**
	 * 생성자에서 키와 유효시간 초기화
	 */
	public JwtTokenProvider(
		@Value("${jwt.secret}") String secret,
		@Value("${jwt.access-token-validity:3600}") long accessTokenValidityInSeconds,
		@Value("${jwt.refresh-token-validity:604800}") long refreshTokenValidityInSeconds
	) {
		// 키 길이 검증
		// TODO: 예외 타입 변경하기
		if (secret.length() < 32) {
			throw new IllegalArgumentException(
				"JWT secret must be at least 256 bits (32 characters) long for security. Current length: "
					+ secret.length()
			);
		}

		// jjwt 0.12.3에서는 키 생성이 더 엄격해짐
		// UTF-8 인코딩을 명시적으로 사용하여 플랫폼 독립성 보장
		this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
		this.accessTokenValidityInSeconds = accessTokenValidityInSeconds;
		this.refreshTokenValidityInSeconds = refreshTokenValidityInSeconds;

		log.info("JWT Token Provider initialized with HS256 algorithm");
	}

	/**
	 * Access Token 생성
	 */
	public String createAccessToken(Long memberId, String loginId) {
		// Java 8 Time API 사용으로 시간 처리가 더 정확해짐
		Instant now = Instant.now();
		Instant expiration = now.plus(accessTokenValidityInSeconds, ChronoUnit.SECONDS);

		try {
			return Jwts.builder()
				// 표준 클레임들 설정
				.subject(loginId)                           // JWT subject - 사용자 식별자
				.issuedAt(Date.from(now))                   // 발급 시간
				.expiration(Date.from(expiration))          // 만료 시간
				.issuer("tissue-api")                       // 발급자 정보

				// 커스텀 클레임들 설정
				.claim("memberId", memberId)                // member id
				.claim("tokenType", "access")        // 토큰 타입
				.claim("loginId", loginId)                  // login id

				// SecretKey 타입에 따라 자동으로 적절한 알고리즘 선택 (HS256)
				.signWith(secretKey)

				// 토큰 문자열로 압축
				.compact();

		} catch (Exception e) {
			log.error("Failed to create access token for loginId: {}", loginId, e);
			throw new RuntimeException("Access token creation failed", e);
		}
	}

	/**
	 * Refresh Token 생성
	 */
	public String createRefreshToken(String loginId) {
		Instant now = Instant.now();
		Instant expiration = now.plus(refreshTokenValidityInSeconds, ChronoUnit.SECONDS);

		try {
			return Jwts.builder()
				.subject(loginId)
				.issuedAt(Date.from(now))
				.expiration(Date.from(expiration))
				.issuer("tissue-api")

				// Refresh Token에는 최소한의 정보만 포함
				.claim("tokenType", "refresh")
				.claim("jti", java.util.UUID.randomUUID().toString()) // JWT ID - 토큰 순환 시 추적용

				.signWith(secretKey)
				.compact();

		} catch (Exception e) {
			log.error("Failed to create refresh token for loginId: {}", loginId, e);
			throw new RuntimeException("Refresh token creation failed", e);
		}
	}

	/**
	 * 토큰에서 로그인 ID 추출
	 */
	public String getLoginIdFromToken(String token) {
		try {
			Claims claims = parseAndValidateClaims(token);
			return claims.getSubject();
		} catch (Exception e) {
			log.warn("Failed to extract loginId from token", e);
			throw new JwtAuthenticationException("Invalid token: cannot extract loginId", e);
		}
	}

	/**
	 * 토큰에서 회원 ID 추출
	 */
	public Long getMemberIdFromToken(String token) {
		try {
			Claims claims = parseAndValidateClaims(token);

			// 타입 안전성 강화: 명시적 타입 검증
			Object memberIdClaim = claims.get("memberId");
			if (memberIdClaim == null) {
				throw new JwtAuthenticationException("Token does not contain memberId claim");
			}

			// 다양한 숫자 타입 처리 (Integer, Long 등)
			if (memberIdClaim instanceof Number) {
				return ((Number)memberIdClaim).longValue();
			}

			throw new JwtAuthenticationException("Invalid memberId claim type: " + memberIdClaim.getClass());

		} catch (Exception e) {
			log.warn("Failed to extract memberId from token", e);
			throw new JwtAuthenticationException("Invalid token: cannot extract memberId", e);
		}
	}

	/**
	 * 토큰 타입 확인 (access/refresh 구분)
	 */
	public String getTokenType(String token) {
		try {
			Claims claims = parseAndValidateClaims(token);
			return claims.get("tokenType", String.class);
		} catch (Exception e) {
			log.warn("Failed to extract token type from token", e);
			throw new JwtAuthenticationException("Invalid token: cannot extract token type", e);
		}
	}

	/**
	 * Access Token 유효성 검증
	 * 검증 항목
	 * 1. 토큰 형식과 서명 유효성
	 * 2. 만료 시간 확인
	 * 3. 토큰 타입 확인 (access인지)
	 * 4. 필수 클레임 존재 여부
	 */
	public boolean validateAccessToken(String token) {
		try {
			Claims claims = parseAndValidateClaims(token);

			// 토큰 타입 확인
			String tokenType = claims.get("tokenType", String.class);
			if (!"access".equals(tokenType)) {
				log.debug("Token validation failed: not an access token. Type: {}", tokenType);
				return false;
			}

			// 필수 클레임 존재 여부 확인
			if (claims.getSubject() == null || claims.get("memberId") == null) {
				log.debug("Token validation failed: missing required claims");
				return false;
			}

			// 만료 시간은 parseAndValidateClaims에서 이미 확인됨
			return true;

		} catch (ExpiredJwtException e) {
			log.debug("Token validation failed: token expired");
			return false;
		} catch (JwtException e) {
			log.debug("Token validation failed: {}", e.getMessage());
			return false;
		} catch (Exception e) {
			log.warn("Unexpected error during token validation", e);
			return false;
		}
	}

	/**
	 * Refresh Token 유효성 검증
	 */
	public boolean validateRefreshToken(String token) {
		try {
			Claims claims = parseAndValidateClaims(token);

			String tokenType = claims.get("tokenType", String.class);
			return "refresh".equals(tokenType);

		} catch (ExpiredJwtException e) {
			log.debug("Refresh token validation failed: token expired");
			return false;
		} catch (JwtException e) {
			log.debug("Refresh token validation failed: {}", e.getMessage());
			return false;
		} catch (Exception e) {
			log.warn("Unexpected error during refresh token validation", e);
			return false;
		}
	}

	/**
	 * 토큰의 남은 수명 계산 (초 단위)
	 * 클라이언트에서 토큰 갱신 시점 결정에 사용
	 */
	public long getTokenRemainingSeconds(String token) {
		try {
			Claims claims = parseAndValidateClaims(token);
			Date expiration = claims.getExpiration();

			long remainingMillis = expiration.getTime() - System.currentTimeMillis();
			return Math.max(0, remainingMillis / 1000);

		} catch (Exception e) {
			log.debug("Failed to calculate token remaining time", e);
			return 0;
		}
	}

	/**
	 * 토큰 파싱 및 기본 검증
	 */
	private Claims parseAndValidateClaims(String token) {
		try {
			return Jwts.parser()
				.verifyWith(secretKey)
				.build()
				.parseSignedClaims(token)
				.getPayload();

		} catch (ExpiredJwtException e) {
			// 만료된 토큰 - 이 예외는 상위 메서드에서 처리
			throw e;
		} catch (UnsupportedJwtException e) {
			log.debug("Unsupported JWT token: {}", e.getMessage());
			throw new JwtAuthenticationException("Unsupported JWT token", e);
		} catch (MalformedJwtException e) {
			log.debug("Malformed JWT token: {}", e.getMessage());
			throw new JwtAuthenticationException("Malformed JWT token", e);
		} catch (SecurityException e) {
			log.debug("Invalid JWT signature: {}", e.getMessage());
			throw new JwtAuthenticationException("Invalid JWT signature", e);
		} catch (IllegalArgumentException e) {
			log.debug("JWT token compact of handler are invalid: {}", e.getMessage());
			throw new JwtAuthenticationException("Invalid JWT token", e);
		}
	}

}
