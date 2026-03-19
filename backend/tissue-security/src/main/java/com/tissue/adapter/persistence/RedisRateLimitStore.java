package com.tissue.adapter.persistence;

import com.tissue.application.port.repository.RateLimitStore;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "tissue.use-redis", havingValue = "true")
@RequiredArgsConstructor
public class RedisRateLimitStore implements RateLimitStore {

    private final StringRedisTemplate redisTemplate;

    @Override
    public int incrementAndGet(String key, Duration window) {
        Long count = redisTemplate.opsForValue().increment(key);
        // TODO: do i really need null checking defensive programming?
        //  if somethings wrong, a exception should already happen at increment()
        if (count != null && count == 1) {
            redisTemplate.expire(key, window);
        }
        return count != null ? count.intValue() : 0;
    }

    @Override
    public int getCount(String key) {
        String value = redisTemplate.opsForValue().get(key);
        return value == null ? 0 : Integer.parseInt(value);
    }

    @Override
    public void reset(String key) {
        redisTemplate.delete(key);
    }
}
