package com.tissue.member.application.port.out;

import java.time.Duration;
import org.jspecify.annotations.Nullable;

// TODO: Add javadoc
public interface EmailVerificationRepository {

    // returns verificationId, stores email & token mapping
    String startVerification(String email, String emailToken, Duration ttl);

    boolean verifyByToken(String emailToken);

    // verifiaction status by verificationId
    VerificationStatus getStatus(String verificationId);

    // validate signup token for final registration
    boolean validateSignupToken(String email, String signupToken);

    void deleteVerification(String verificationId);

    record VerificationStatus(String status, @Nullable String signupToken) {}
}
