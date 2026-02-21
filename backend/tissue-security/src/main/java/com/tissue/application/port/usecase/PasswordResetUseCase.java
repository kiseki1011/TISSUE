package com.tissue.application.port.usecase;

public interface PasswordResetUseCase {
    /**
     * Step 1: Send a 6-digit verification code to the user's email.
     */
    void requestPasswordReset(String email);

    /**
     * Step 2: Verify the 6-digit code and return a reset token.
     */
    String verifyResetCode(String email, String code);

    /**
     * Step 3: Reset the password using the reset token.
     */
    void resetPassword(String resetToken, String newPassword);
}
