package com.tissue.application.port.usecase;

import com.tissue.application.port.repository.EmailVerificationRepository.VerificationStatus;

public interface PasswordResetUseCase {

    String requestPasswordReset(String email);

    boolean verifyEmailToken(String emailToken);

    VerificationStatus getVerificationStatus(String verificationId);

    void resetPassword(String resetToken, String newPassword);
}
