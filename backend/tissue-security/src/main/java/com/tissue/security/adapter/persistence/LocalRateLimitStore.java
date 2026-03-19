package com.tissue.security.adapter.persistence;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tissue.security.application.port.repository.RateLimitStore;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "tissue.use-redis", havingValue = "false", matchIfMissing = true)
public class LocalRateLimitStore implements RateLimitStore {

    private final Cache<String, RateLimitEntry> cache = Caffeine.newBuilder()
            .expireAfterWrite(2, TimeUnit.HOURS)
            .maximumSize(100_000)
            .build();

    @Override
    public int incrementAndGet(String key, Duration window) {
        RateLimitEntry entry = cache.asMap().compute(key, (k, existing) -> {
            if (existing == null || existing.isExpired()) {
                return new RateLimitEntry(new AtomicInteger(1), Instant.now().plus(window));
            }
            existing.count().incrementAndGet();
            return existing;
        });
        return entry.count().get();
    }

    @Override
    public int getCount(String key) {
        RateLimitEntry entry = cache.getIfPresent(key);
        if (entry == null || entry.isExpired()) {
            return 0;
        }
        return entry.count().get();
    }

    @Override
    public void reset(String key) {
        cache.invalidate(key);
    }

    private record RateLimitEntry(AtomicInteger count, Instant expiresAt) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
