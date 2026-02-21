package com.tissue.application.port.usecase;

import com.tissue.application.port.repository.EmailVerificationRepository.VerificationStatus;

public interface PasswordResetUseCase {
    /**
     * Step 1: Send a reset link to the user's email.
     * Returns a verificationId for polling.
     */
    String requestPasswordReset(String email);

    /**
     * Step 2: Handle the link click from the email.
     */
    void verifyEmailToken(String emailToken);

    /**
     * Step 3: Polling endpoint to check if the user has clicked the link.
     */
    VerificationStatus getVerificationStatus(String verificationId);

    /**
     * Step 4: Final reset using the verifiedToken.
     */
    void resetPassword(String resetToken, String newPassword);
}
