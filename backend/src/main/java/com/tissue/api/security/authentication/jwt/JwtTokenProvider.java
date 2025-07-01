package com.tissue.api.security.authentication.jwt;

import static com.tissue.api.security.util.MaskingUtil.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import com.tissue.api.security.authentication.MemberUserDetailsService;
import com.tissue.api.security.authentication.exception.JwtAuthenticationException;
import com.tissue.api.security.authentication.exception.JwtCreationException;
import com.tissue.api.security.authentication.exception.JwtSecretException;

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

	public static final String CLAIM_NAME_TOKEN_TYPE = "tokenType";
	public static final String CLAIM_NAME_MEMBER_ID = "memberId";
	public static final String TOKEN_TYPE_ACCESS = "access";
	public static final String TOKEN_TYPE_REFRESH = "refresh";
	public static final String ISSUER = "tissue-api";
	public static final String CLAIM_NAME_JTI = "jti";
	public static final int SECRET_KEY_LENGTH = 32;

	private final SecretKey secretKey;
	private final long accessTokenValidityInSeconds;
	private final long refreshTokenValidityInSeconds;
	private final MemberUserDetailsService userDetailsService;

	/**
	 * 생성자에서 키와 유효시간 초기화
	 */
	// TODO: JwtProperties 클래스를 만들어서 사용하는 것을 고려
	public JwtTokenProvider(
		@Value("${jwt.secret}") String secret,
		@Value("${jwt.access-token-validity:3600}") long accessTokenValidityInSeconds,
		@Value("${jwt.refresh-token-validity:604800}") long refreshTokenValidityInSeconds,
		MemberUserDetailsService userDetailsService
	) {
		this.userDetailsService = userDetailsService;

		// 키 길이 검증
		if (secret.length() < SECRET_KEY_LENGTH) {
			throw new JwtSecretException(
				"JWT secret must be at least 256 bits (32 characters) long for security. Current length: "
					+ secret.length()
			);
		}

		this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.accessTokenValidityInSeconds = accessTokenValidityInSeconds;
		this.refreshTokenValidityInSeconds = refreshTokenValidityInSeconds;

		log.info("JWT Token Provider initialized with HS256 algorithm.");
	}

	/**
	 * Create Access Token
	 * - subject: loginId
	 * - memberId: Primary Key for Member
	 * - tokenType: "access"
	 */
	public String createAccessToken(Long memberId, String loginIdentifier) {
		Instant now = Instant.now();
		Instant expiration = now.plus(accessTokenValidityInSeconds, ChronoUnit.SECONDS);

		try {
			return Jwts.builder()
				.subject(loginIdentifier) // JWT subject - 사용자 식별자
				.issuedAt(Date.from(now)) // 발급 시간
				.expiration(Date.from(expiration)) // 만료 시간
				.issuer(ISSUER) // 발급자 정보
				.claim(CLAIM_NAME_MEMBER_ID, memberId) // member id
				.claim(CLAIM_NAME_TOKEN_TYPE, TOKEN_TYPE_ACCESS) // 토큰 타입
				.signWith(secretKey) // SecretKey 타입에 따라 자동으로 적절한 알고리즘 선택 (HS256)
				.compact(); // 토큰 문자열로 압축
		} catch (JwtException | IllegalArgumentException e) {
			throw new JwtCreationException(
				"Failed to create access token for loginIdentifier: " + maskIdentifier(loginIdentifier),
				e);
		}
	}

	/**
	 * Refresh Token 생성
	 */
	public String createRefreshToken(String loginIdentifier) {
		Instant now = Instant.now();
		Instant expiration = now.plus(refreshTokenValidityInSeconds, ChronoUnit.SECONDS);

		try {
			return Jwts.builder()
				.subject(loginIdentifier)
				.issuedAt(Date.from(now))
				.expiration(Date.from(expiration))
				.issuer(ISSUER)
				.claim(CLAIM_NAME_TOKEN_TYPE, TOKEN_TYPE_REFRESH)
				.claim(CLAIM_NAME_JTI, UUID.randomUUID().toString()) // JWT ID - 토큰 순환 시 추적용
				.signWith(secretKey)
				.compact();

		} catch (JwtException | IllegalArgumentException e) {
			throw new JwtCreationException(
				"Failed to create refresh token for loginIdentifier: " + maskIdentifier(loginIdentifier), e);
		}
	}

	/**
	 * JWT 토큰으로부터 Authentication 객체 생성
	 *
	 * 처리 흐름:
	 * 1. 토큰 유효성 검증 (형식, 서명, 만료시간)
	 * 2. 토큰에서 사용자 식별자 추출
	 * 3. UserDetailsService를 통한 사용자 정보 조회 (실시간 상태 확인)
	 * 4. 완전한 Authentication 객체 생성 및 반환, 검증 실패시 Optional.empty()
	 */
	// TODO: token 마스킹을 위한 유틸 클래스 구현 및 사용
	public Optional<Authentication> getAuthentication(String token) {
		String loginIdentifier = null;

		try {
			// 토큰 기본 검증 (형식, 서명, 만료시간, 토큰 타입)
			if (!validateAccessToken(token)) {
				log.debug("Token validation failed for token: {}", token);
				return Optional.empty();
			}

			// 토큰에서 사용자 식별자 추출
			loginIdentifier = getLoginIdFromToken(token);

			// 사용자 정보 조회 및 현재 상태 확인
			// - 이 단계에서 사용자가 탈퇴했거나 비활성화된 경우를 실시간으로 확인
			UserDetails userDetails = userDetailsService.loadUserByUsername(loginIdentifier);

			// Authentication 객체 생성
			// - principal: 사용자 정보 (UserDetails 구현체)
			// - credentials: null (JWT 토큰 자체가 이미 인증 수단이므로)
			// - authorities: 사용자의 권한 목록
			UsernamePasswordAuthenticationToken authentication =
				new UsernamePasswordAuthenticationToken(
					userDetails,
					null,  // JWT에서는 비밀번호 필요 없음
					userDetails.getAuthorities()
				);

			log.debug("Successfully created authentication for user with loginIdentifier: {}",
				maskIdentifier(loginIdentifier));

			return Optional.of(authentication);

		} catch (UsernameNotFoundException e) {
			throw new JwtAuthenticationException(
				"Member not found for loginIdentifier: " + maskIdentifier(loginIdentifier), e);
		}
	}

	/**
	 * 토큰에서 로그인 ID 추출
	 */
	public String getLoginIdFromToken(String token) {
		try {
			Claims claims = parseAndValidateClaims(token);
			return claims.getSubject();

		} catch (JwtException e) {
			throw new JwtAuthenticationException("Failed to extract loginId from token.", e);
		}
	}

	/**
	 * 토큰에서 회원 ID 추출
	 */
	public Long getMemberIdFromToken(String token) {
		try {
			Claims claims = parseAndValidateClaims(token);

			// 타입 검증
			Object memberIdClaim = claims.get(CLAIM_NAME_MEMBER_ID);
			if (memberIdClaim == null) {
				throw new JwtAuthenticationException("Token does not contain memberId claim.");
			}

			// 다양한 숫자 타입 처리 (Integer, Long 등)
			if (memberIdClaim instanceof Number) {
				return ((Number)memberIdClaim).longValue();
			}

			throw new JwtAuthenticationException("Invalid memberId claim type: " + memberIdClaim.getClass());

		} catch (JwtException e) {
			log.warn("Failed to extract memberId from token.", e);
			throw new JwtAuthenticationException("Cannot extract memberId from token.", e);
		}
	}

	/**
	 * 토큰 타입 확인 (access/refresh 구분)
	 */
	public String getTokenType(String token) {
		try {
			Claims claims = parseAndValidateClaims(token);
			return claims.get(CLAIM_NAME_TOKEN_TYPE, String.class);

		} catch (JwtException e) {
			log.warn("Failed to extract tokenType from token.", e);
			throw new JwtAuthenticationException("Cannot extract tokenType from token.", e);
		}
	}

	/**
	 * Access Token 유효성 검증
	 *
	 * 검증 항목:
	 * 1. 토큰 형식과 서명 유효성
	 * 2. 만료 시간 확인
	 * 3. 토큰 타입 확인 (access인지)
	 * 4. 필수 클레임 존재 여부
	 */
	public boolean validateAccessToken(String token) {
		try {
			Claims claims = parseAndValidateClaims(token);

			// 토큰 타입 확인
			String tokenType = claims.get(CLAIM_NAME_TOKEN_TYPE, String.class);
			if (!Objects.equals(TOKEN_TYPE_ACCESS, tokenType)) {
				log.debug("Token validation failed: not an access token. Type: {}", tokenType);
				return false;
			}

			// 필수 클레임 존재 여부 확인
			if (claims.getSubject() == null || claims.get(CLAIM_NAME_MEMBER_ID) == null) {
				log.debug("Token validation failed: missing required claims.");
				return false;
			}

			return true;

		} catch (ExpiredJwtException e) {
			log.debug("Access token is expired: {}", e.getMessage());
			return false;
		} catch (JwtException e) {
			log.warn("Access token is invalid: {}, token: {}", e.getMessage(), maskToken(token));
			return false;
		}
	}

	/**
	 * Refresh Token 유효성 검증
	 */
	public boolean validateRefreshToken(String token) {
		try {
			Claims claims = parseAndValidateClaims(token);

			String tokenType = claims.get(CLAIM_NAME_TOKEN_TYPE, String.class);
			return Objects.equals(TOKEN_TYPE_REFRESH, tokenType);

		} catch (ExpiredJwtException e) {
			log.info("Refresh token is expired: {}", e.getMessage());
			return false;
		} catch (JwtException e) {
			log.warn("Refresh token is invalid: {}, token: {}", e.getMessage(), maskToken(token));
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

		} catch (JwtException e) {
			log.debug("Failed to calculate remaining time for token.", e);
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
			// 만료된 토큰 - 상위 메서드에서 처리
			log.debug("JWT token is expired: {}, token: {}", e.getMessage(), maskToken(token));
			throw e;
			// TODO: instanceOf를 사용한 분기문으로 중복 제거? 케이스별로 reason을 추출해서 사용.
		} catch (UnsupportedJwtException e) {
			log.debug("JWT token is unsupported: {}, token: {}", e.getMessage(), maskToken(token));
			throw new JwtAuthenticationException("Unsupported JWT token.", e);
		} catch (MalformedJwtException e) {
			log.debug("JWT token is malformed: {}, token: {}", e.getMessage(), maskToken(token));
			throw new JwtAuthenticationException("Malformed JWT token.", e);
		} catch (SecurityException e) {
			log.debug("JWT token signature is invalid: {}, token: {}", e.getMessage(), maskToken(token));
			throw new JwtAuthenticationException("Invalid JWT signature.", e);
		} catch (IllegalArgumentException e) {
			log.debug("JWT token is illegal or empty: {}, token: {}", e.getMessage(), maskToken(token));
			throw new JwtAuthenticationException("Invalid JWT token.", e);
		}
	}
}
