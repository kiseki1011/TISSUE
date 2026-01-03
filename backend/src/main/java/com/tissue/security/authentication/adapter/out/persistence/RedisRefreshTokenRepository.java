package com.tissue.security.authentication.adapter.out.persistence;

import com.tissue.security.authentication.application.port.out.RefreshTokenRepository;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RedisRefreshTokenRepository implements RefreshTokenRepository {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String PREFIX = "refresh_token:";

    @Override
    public void save(String email, String refreshToken, Duration ttl) {
        redisTemplate.opsForValue().set(PREFIX + email, refreshToken, ttl);
    }

    @Override
    public java.util.Optional<String> findByEmail(String email) {
        String token = redisTemplate.opsForValue().get(PREFIX + email);
        return java.util.Optional.ofNullable(token);
    }

    @Override
    public void deleteByEmail(String email) {
        redisTemplate.delete(PREFIX + email);
    }
}
