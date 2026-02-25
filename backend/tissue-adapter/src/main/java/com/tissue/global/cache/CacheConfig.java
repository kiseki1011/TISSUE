package com.tissue.global.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_ISSUE_FIELDS = "issueFields";

    /**
     * Local Cache Manager - Caffeine
     */
    @Bean
    public CacheManager localCacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();

        // short TTL only to support batch operations
        CaffeineCache issueFieldsCache = new CaffeineCache(
                CACHE_ISSUE_FIELDS,
                Caffeine.newBuilder()
                        .expireAfterWrite(1, TimeUnit.MINUTES)
                        .maximumSize(500)
                        .build());

        CaffeineCache generalCache = new CaffeineCache(
                "localGeneral",
                Caffeine.newBuilder()
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .maximumSize(1000)
                        .build());

        cacheManager.setCaches(List.of(issueFieldsCache, generalCache));
        return cacheManager;
    }

    /**
     * Primary Cache Manager - Redis
     *
     * <p>Activated when {@code tissue.cache.type=redis}</p>
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "tissue.cache.type", havingValue = "redis")
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }

    /**
     * Primary Cache Manager - Caffeine (Fallback)
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "tissue.cache.type", havingValue = "caffeine", matchIfMissing = true)
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(
                Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.MINUTES).maximumSize(5000));
        return cacheManager;
    }
}
