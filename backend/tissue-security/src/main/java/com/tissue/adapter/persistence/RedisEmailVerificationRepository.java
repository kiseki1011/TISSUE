package com.tissue.adapter.persistence;

import com.tissue.application.port.repository.EmailVerificationRepository;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "tissue.email.verification.token-store", havingValue = "redis")
@RequiredArgsConstructor
public class RedisEmailVerificationRepository implements EmailVerificationRepository {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String KEY_REQUEST = "verification:request:";
    private static final String KEY_TOKEN = "verification:token:";
    private static final String KEY_SIGNUP = "verification:signup:";

    private static final String FIELD_EMAIL = "email";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_SIGNUP_TOKEN = "signupToken";

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_VERIFIED = "VERIFIED";

    @Override
    public String startVerification(String email, String emailToken, Duration ttl) {
        String verificationId = UUID.randomUUID().toString();

        redisTemplate.opsForValue().set(KEY_TOKEN + emailToken, verificationId, ttl);

        String requestKey = KEY_REQUEST + verificationId;
        redisTemplate.opsForHash().put(requestKey, FIELD_EMAIL, email);
        redisTemplate.opsForHash().put(requestKey, FIELD_STATUS, STATUS_PENDING);
        redisTemplate.expire(requestKey, ttl);

        return verificationId;
    }

    @Override
    public boolean verifyByToken(String emailToken, Duration signupTokenTtl) {
        String verificationId = redisTemplate.opsForValue().get(KEY_TOKEN + emailToken);
        if (verificationId == null) {
            return false;
        }

        String requestKey = KEY_REQUEST + verificationId;
        String email = (String) redisTemplate.opsForHash().get(requestKey, FIELD_EMAIL);

        if (email == null) {
            return false;
        }

        String signupToken = UUID.randomUUID().toString();

        redisTemplate.opsForHash().put(requestKey, FIELD_STATUS, STATUS_VERIFIED);
        redisTemplate.opsForHash().put(requestKey, FIELD_SIGNUP_TOKEN, signupToken);

        redisTemplate.opsForValue().set(KEY_SIGNUP + signupToken, email, signupTokenTtl);

        redisTemplate.delete(KEY_TOKEN + emailToken);

        return true;
    }

    @Override
    public VerificationStatus getStatus(String verificationId) {
        String requestKey = KEY_REQUEST + verificationId;
        String status = (String) redisTemplate.opsForHash().get(requestKey, FIELD_STATUS);
        String signupToken = (String) redisTemplate.opsForHash().get(requestKey, FIELD_SIGNUP_TOKEN);

        if (status == null) {
            return new VerificationStatus("UNKNOWN", null);
        }
        return new VerificationStatus(status, signupToken);
    }

    @Override
    public boolean validateSignupToken(String email, String signupToken) {
        String storedEmail = redisTemplate.opsForValue().get(KEY_SIGNUP + signupToken);
        if (Objects.equals(storedEmail, email)) {
            redisTemplate.delete(KEY_SIGNUP + signupToken);
            return true;
        }
        return false;
    }

    @Override
    public void deleteVerification(String verificationId) {
        redisTemplate.delete(KEY_REQUEST + verificationId);
    }
}
