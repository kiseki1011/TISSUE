package com.tissue.application.port.repository;

import java.time.Duration;
import org.jspecify.annotations.Nullable;

public interface EmailVerificationRepository {

    /**
     * Starts the verification flow by mapping an email to its tokens.
     *
     * @param email The email address to verify.
     * @param emailToken The secret token sent via email link.
     * @param ttl Duration for which this verification remains valid.
     * @return A unique verificationId for the client to poll the status.
     */
    String startVerification(String email, String emailToken, Duration ttl);

    /**
     * Validates the email token.
     * On success, a signupToken should be generated and saved (either as an entity or in a memory-db).
     *
     * @param emailToken The secret token from the email link.
     * @param signupTokenTtl Duration for which the resulting signupToken remains valid.
     * @return true if the token is valid and not expired, false otherwise.
     */
    boolean verifyByToken(String emailToken, Duration signupTokenTtl);

    /**
     * Retrieves the current status of a verification request.
     * Used for polling from the client side.
     *
     * @param verificationId The secure request ID.
     * @return Current status and an optional signupToken if verified.
     */
    VerificationStatus getStatus(String verificationId);

    /**
     * Performs final validation before member registration.
     * The token should be consumed (deleted) upon successful validation to prevent replay attacks.
     *
     * @param email The email being registered.
     * @param signupToken The final proof of email verification.
     * @return true if the token matches the email and is valid.
     */
    boolean validateSignupToken(String email, String signupToken);

    /**
     * Deletes a verification token.
     *
     * @param verificationId The verification ID of the verification token to remove.
     */
    void deleteVerification(String verificationId);

    /**
     * Represents the status of the verification request.
     *
     * @param status Current status of verification ("PENDING", "VERIFIED", "UNKNOWN").
     * @param signupToken The token required for final signup, only available if status is "VERIFIED".
     */
    record VerificationStatus(String status, @Nullable String signupToken) {}
}
