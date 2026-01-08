package com.tissue.member.adapter.out.persistence;

import com.tissue.member.application.port.out.EmailVerificationRepository;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "tissue.email.verification.strategy", havingValue = "redis")
@RequiredArgsConstructor
public class RedisEmailVerificationRepository implements EmailVerificationRepository {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${tissue.email.verification.ttl}")
    private Duration ttl;

    private static final String PREFIX = "email_verification:";
    private static final String VERIFIED_SUFFIX = ":verified";

    @Override
    public void saveToken(String email, String tokenValue, Duration ttl) {
        redisTemplate.opsForValue().set(PREFIX + email, tokenValue, ttl);
    }

    @Override
    public boolean verify(String email, String tokenValue) {
        String storedValue = redisTemplate.opsForValue().get(PREFIX + email);

        log.debug("Stored token: {}, input token: {}", storedValue, tokenValue);

        if (storedValue == null || !storedValue.equals(tokenValue)) {
            return false;
        }

        // Mark as verified by appending suffix, but keep the original token
        redisTemplate.opsForValue().set(PREFIX + email, tokenValue + VERIFIED_SUFFIX, ttl);
        return true;
    }

    @Override
    public boolean isVerified(String email) {
        String storedValue = redisTemplate.opsForValue().get(PREFIX + email);
        return storedValue != null && storedValue.endsWith(VERIFIED_SUFFIX);
    }

    @Override
    public boolean checkVerifiedToken(String email, String token) {
        String storedValue = redisTemplate.opsForValue().get(PREFIX + email);
        return storedValue != null && storedValue.equals(token + VERIFIED_SUFFIX);
    }

    @Override
    public void deleteToken(String email) {
        redisTemplate.delete(PREFIX + email);
    }
}
