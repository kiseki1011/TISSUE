package com.tissue.adapter.persistence;

import com.tissue.application.port.repository.RefreshTokenRepository;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "tissue.security.refresh-token.store", havingValue = "redis")
@RequiredArgsConstructor
public class RedisRefreshTokenRepository implements RefreshTokenRepository {

    private static final String PREFIX = "refresh_token:";

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void save(String email, String refreshToken, Duration ttl) {
        redisTemplate.opsForValue().set(PREFIX + email, refreshToken, ttl);
    }

    @Override
    public Optional<String> findByEmail(String email) {
        String token = redisTemplate.opsForValue().get(PREFIX + email);
        return Optional.ofNullable(token);
    }

    @Override
    public void deleteByEmail(String email) {
        redisTemplate.delete(PREFIX + email);
    }
}
