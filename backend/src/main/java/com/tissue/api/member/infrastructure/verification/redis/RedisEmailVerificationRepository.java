package com.tissue.api.member.infrastructure.verification.redis;

import java.time.Duration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.tissue.api.member.domain.repository.verification.EmailVerificationRepository;

import lombok.RequiredArgsConstructor;

@Component
@ConditionalOnProperty(name = "email.verification.strategy", havingValue = "redis")
@RequiredArgsConstructor
public class RedisEmailVerificationRepository implements EmailVerificationRepository {

	private final RedisTemplate<String, String> redisTemplate;

	private static final String PREFIX = "email_verification:";

	@Override
	public void saveToken(String email, String tokenValue, Duration ttl) {
		redisTemplate.opsForValue().set(PREFIX + email, tokenValue, ttl);
	}

	// TODO: "verified" 상수화
	@Override
	public boolean verify(String email, String tokenValue) {
		String stored = redisTemplate.opsForValue().get(PREFIX + email);

		if (!tokenValue.equals(stored)) {
			return false;
		}

		redisTemplate.opsForValue().set(PREFIX + email, "verified", ttl());
		return true;
	}

	@Override
	public boolean isVerified(String email) {
		String value = redisTemplate.opsForValue().get(PREFIX + email);
		return "verified".equals(value);
	}

	// TODO: @ConfigurationProperties(prefix = "email.verification")를 사용해서 TTL 값 관리
	private Duration ttl() {
		return Duration.ofMinutes(30);
	}
}
