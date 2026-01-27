package com.tissue.member.adapter.out.persistence;

import com.tissue.member.application.port.out.EmailVerificationRepository;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "tissue.email.verification.strategy", havingValue = "redis")
@RequiredArgsConstructor
public class RedisEmailVerificationRepository implements EmailVerificationRepository {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${tissue.email.verification.ttl}")
    private Duration ttl;

    // verification:request:{verificationId} -> Hash(email, status, signupToken) - used for polling
    // verification:token:{emailToken} -> verificationId - used for Email Link
    // verification:signup:{signupToken} -> email - used for Signup Validation

    private static final String KEY_REQUEST = "verification:request:";
    private static final String KEY_TOKEN = "verification:token:";
    private static final String KEY_SIGNUP = "verification:signup:";

    @Override
    public String startVerification(String email, String emailToken, Duration ttl) {
        String verificationId = UUID.randomUUID().toString();

        // Link email token to verification ID
        redisTemplate.opsForValue().set(KEY_TOKEN + emailToken, verificationId, ttl);

        // Init verification request status
        String requestKey = KEY_REQUEST + verificationId;
        redisTemplate.opsForHash().put(requestKey, "email", email);
        redisTemplate.opsForHash().put(requestKey, "status", "PENDING");
        redisTemplate.expire(requestKey, ttl);

        return verificationId;
    }

    @Override
    public boolean verifyByToken(String emailToken) {
        String verificationId = redisTemplate.opsForValue().get(KEY_TOKEN + emailToken);
        if (verificationId == null) {
            return false;
        }

        String requestKey = KEY_REQUEST + verificationId;
        String email = (String) redisTemplate.opsForHash().get(requestKey, "email");

        if (email == null) {
            return false;
        }

        // Generate Secure Signup Token
        String signupToken = UUID.randomUUID().toString();

        // Update Request Status (for Polling)
        redisTemplate.opsForHash().put(requestKey, "status", "VERIFIED");
        redisTemplate.opsForHash().put(requestKey, "signupToken", signupToken);

        // Store Signup Token for final validation (valid for 10 mins)
        redisTemplate.opsForValue().set(KEY_SIGNUP + signupToken, email, Duration.ofMinutes(10));

        // Delete used email token
        redisTemplate.delete(KEY_TOKEN + emailToken);

        return true;
    }

    @Override
    public VerificationStatus getStatus(String verificationId) {
        String requestKey = KEY_REQUEST + verificationId;
        String status = (String) redisTemplate.opsForHash().get(requestKey, "status");
        String signupToken = (String) redisTemplate.opsForHash().get(requestKey, "signupToken");

        if (status == null) {
            return new VerificationStatus("UNKNOWN", null);
        }
        return new VerificationStatus(status, signupToken);
    }

    @Override
    public boolean validateSignupToken(String email, String signupToken) {
        String storedEmail = redisTemplate.opsForValue().get(KEY_SIGNUP + signupToken);
        if (storedEmail != null && storedEmail.equals(email)) {
            // Token used, delete it to prevent replay (Single Use)
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
