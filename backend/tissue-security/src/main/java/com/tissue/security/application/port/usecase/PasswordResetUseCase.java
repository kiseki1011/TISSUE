package com.tissue.security.application.port.usecase;

import com.tissue.security.application.port.repository.EmailVerificationRepository.VerificationStatus;

public interface PasswordResetUseCase {

    String requestPasswordReset(String email);

    boolean verifyEmailToken(String emailToken);

    VerificationStatus getVerificationStatus(String verificationId);

    void resetPassword(String email, String verifiedToken, String newPassword);
}
