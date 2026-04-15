package com.tissue.security.adapter.persistence;

import com.tissue.security.application.port.repository.RateLimitStore;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "tissue.use-redis", havingValue = "true")
@RequiredArgsConstructor
public class RedisRateLimitStore implements RateLimitStore {

    private static final DefaultRedisScript<Long> INCREMENT_SCRIPT = createIncrementScript();

    private final StringRedisTemplate redisTemplate;

    private static DefaultRedisScript<Long> createIncrementScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText("""
                local count = redis.call('INCR', KEYS[1])
                if count == 1 then
                    redis.call('EXPIRE', KEYS[1], ARGV[1])
                end
                return count
                """);
        script.setResultType(Long.class);
        return script;
    }

    @Override
    public int incrementAndGet(String key, Duration window) {
        Long count = redisTemplate.execute(INCREMENT_SCRIPT, List.of(key), String.valueOf(window.toSeconds()));
        return count.intValue();
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
