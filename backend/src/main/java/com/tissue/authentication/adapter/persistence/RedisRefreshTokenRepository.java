package com.tissue.authentication.adapter.persistence;

import com.tissue.authentication.application.port.out.RefreshTokenRepository;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RedisRefreshTokenRepository implements RefreshTokenRepository {

    private final RedisTemplate<String, String> redisTemplate;
    // TODO: To support multi-device login, change the key structure to include deviceId (e.g.,
    //  "refresh_token:email:deviceId")
    //  Currently, a new login invalidates previous sessions because the key is simply "refresh_token:email".
    private static final String PREFIX = "refresh_token:";

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
