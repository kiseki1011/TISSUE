package com.tissue.security.application.port.repository;

import java.time.Duration;
import org.jspecify.annotations.Nullable;

public interface EmailVerificationRepository {

    void storeVerificationContext(String verificationId, String email, String emailToken, Duration ttl);

    boolean verifyByEmailToken(String emailToken, Duration verifiedTokenTtl);

    VerificationStatus getStatus(String verificationId);

    /**
     * Validates and consumes the verifiedToken.
     * Returns the associated email if valid, null otherwise.
     */
    @Nullable
    String validateVerifiedToken(String verifiedToken);

    void deleteVerification(String verificationId);

    record VerificationStatus(String status, @Nullable String verifiedToken) {}
}
