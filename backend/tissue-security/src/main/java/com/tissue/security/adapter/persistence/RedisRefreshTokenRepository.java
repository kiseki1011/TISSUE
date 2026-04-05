package com.tissue.security.adapter.persistence;

import com.tissue.security.application.port.repository.RefreshTokenRepository;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "tissue.use-redis", havingValue = "true")
@RequiredArgsConstructor
public class RedisRefreshTokenRepository implements RefreshTokenRepository {

    private static final String PREFIX = "refresh_token:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void save(Long memberId, String refreshToken, Duration ttl) {
        redisTemplate.opsForValue().set(PREFIX + memberId, refreshToken, ttl);
    }

    @Override
    public Optional<String> findByMemberId(Long memberId) {
        String token = redisTemplate.opsForValue().get(PREFIX + memberId);
        return Optional.ofNullable(token);
    }

    @Override
    public void deleteByMemberId(Long memberId) {
        redisTemplate.delete(PREFIX + memberId);
    }
}
