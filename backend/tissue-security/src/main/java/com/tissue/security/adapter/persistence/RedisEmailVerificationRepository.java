package com.tissue.security.adapter.persistence;

import com.tissue.security.application.port.repository.EmailVerificationRepository;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "tissue.use-redis", havingValue = "true")
@RequiredArgsConstructor
public class RedisEmailVerificationRepository implements EmailVerificationRepository {

    private final StringRedisTemplate redisTemplate;

    private static final String KEY_REQUEST = "verification:request:";
    private static final String KEY_EMAIL_TOKEN = "verification:email-token:";
    private static final String KEY_VERIFIED_TOKEN = "verification:verified-token:";

    private static final String FIELD_EMAIL = "email";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_VERIFIED_TOKEN = "verifiedToken";

    @Override
    public void storeVerificationContext(String verificationId, String email, String emailToken, Duration ttl) {
        redisTemplate.opsForValue().set(KEY_EMAIL_TOKEN + emailToken, verificationId, ttl);

        String requestKey = KEY_REQUEST + verificationId;
        redisTemplate.opsForHash().put(requestKey, FIELD_EMAIL, email);
        redisTemplate.opsForHash().put(requestKey, FIELD_STATUS, Status.PENDING.name());
        redisTemplate.expire(requestKey, ttl);
    }

    @Override
    public boolean verifyByEmailToken(String emailToken, Duration verifiedTokenTtl) {
        String verificationId = redisTemplate.opsForValue().get(KEY_EMAIL_TOKEN + emailToken);
        if (verificationId == null) {
            return false;
        }

        String requestKey = KEY_REQUEST + verificationId;
        String email = (String) redisTemplate.opsForHash().get(requestKey, FIELD_EMAIL);

        if (email == null) {
            return false;
        }

        String verifiedToken = UUID.randomUUID().toString();

        redisTemplate.opsForHash().put(requestKey, FIELD_STATUS, Status.VERIFIED.name());
        redisTemplate.opsForHash().put(requestKey, FIELD_VERIFIED_TOKEN, verifiedToken);

        redisTemplate.opsForValue().set(KEY_VERIFIED_TOKEN + verifiedToken, email, verifiedTokenTtl);

        redisTemplate.delete(KEY_EMAIL_TOKEN + emailToken);

        return true;
    }

    @Override
    public VerificationStatus getStatus(String verificationId) {
        String requestKey = KEY_REQUEST + verificationId;
        String status = (String) redisTemplate.opsForHash().get(requestKey, FIELD_STATUS);
        String verifiedToken = (String) redisTemplate.opsForHash().get(requestKey, FIELD_VERIFIED_TOKEN);

        if (status == null) {
            return new VerificationStatus(Status.UNKNOWN, null);
        }
        return new VerificationStatus(Status.valueOf(status), verifiedToken);
    }

    @Override
    public @Nullable String validateVerifiedToken(String verifiedToken) {
        String storedEmail = redisTemplate.opsForValue().get(KEY_VERIFIED_TOKEN + verifiedToken);
        if (storedEmail != null) {
            redisTemplate.delete(KEY_VERIFIED_TOKEN + verifiedToken);
            return storedEmail;
        }
        return null;
    }
}
