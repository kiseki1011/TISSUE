package com.tissue.security.application.service;

import com.tissue.security.application.port.repository.RateLimitStore;
import com.tissue.security.config.RateLimitProperties;
import com.tissue.security.domain.exception.AuthenticationErrorCode;
import com.tissue.shared.exception.CommonErrorCode;
import com.tissue.shared.exception.base.RateLimitExceededException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final RateLimitStore rateLimitStore;
    private final RateLimitProperties properties;

    private static final String LOGIN_PREFIX = "rate:login:";
    private static final String EMAIL_VERIFICATION_PREFIX = "rate:signup-verify:";
    private static final String PASSWORD_RESET_PREFIX = "rate:password-reset:";

    public void checkLoginRateLimit(String clientIp, String identifier) {
        String key = LOGIN_PREFIX + clientIp + ":" + identifier;
        int count = rateLimitStore.incrementAndGet(key, properties.getLogin().getWindow());
        if (count > properties.getLogin().getMaxAttempts()) {
            throw new RateLimitExceededException(AuthenticationErrorCode.LOGIN_RATE_LIMITED);
        }
    }

    public void resetLoginAttempts(String clientIp, String email) {
        String key = LOGIN_PREFIX + clientIp + ":" + email;
        rateLimitStore.reset(key);
    }

    public void checkEmailVerificationRateLimit(String email) {
        RateLimitProperties.EmailVerification config = properties.getEmailVerification();
        String key = EMAIL_VERIFICATION_PREFIX + email;
        int count = rateLimitStore.incrementAndGet(key, config.getWindow());
        if (count > config.getMaxAttempts()) {
            throw new RateLimitExceededException(CommonErrorCode.RATE_LIMITED);
        }
    }

    public void checkPasswordResetRateLimit(String email) {
        RateLimitProperties.PasswordReset config = properties.getPasswordReset();
        String key = PASSWORD_RESET_PREFIX + email;
        int count = rateLimitStore.incrementAndGet(key, config.getWindow());
        if (count > config.getMaxAttempts()) {
            throw new RateLimitExceededException(CommonErrorCode.RATE_LIMITED);
        }
    }
}
