package com.tissue.application.service;

import com.tissue.application.port.repository.RateLimitStore;
import com.tissue.config.RateLimitProperties;
import com.tissue.domain.exception.AuthenticationErrorCode;
import com.tissue.shared.exception.base.RateLimitExceededException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final RateLimitStore rateLimitStore;
    private final RateLimitProperties properties;

    private static final String LOGIN_PREFIX = "rate:login:";

    public void checkLoginRateLimit(String clientIp, String email) {
        String key = LOGIN_PREFIX + clientIp + ":" + email;
        int count = rateLimitStore.getCount(key);
        if (count >= properties.getLogin().getMaxAttempts()) {
            throw new RateLimitExceededException(AuthenticationErrorCode.LOGIN_RATE_LIMITED);
        }
    }

    public void recordLoginFailure(String clientIp, String email) {
        String key = LOGIN_PREFIX + clientIp + ":" + email;
        rateLimitStore.incrementAndGet(key, properties.getLogin().getWindow());
    }

    public void resetLoginAttempts(String clientIp, String email) {
        String key = LOGIN_PREFIX + clientIp + ":" + email;
        rateLimitStore.reset(key);
    }
}
