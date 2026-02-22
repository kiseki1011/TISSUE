package com.tissue.application.service;

import com.tissue.application.port.repository.AuthenticationIdentityRepository;
import com.tissue.application.port.repository.EmailVerificationRepository.VerificationStatus;
import com.tissue.application.port.usecase.PasswordResetUseCase;
import com.tissue.domain.AuthenticationIdentity;
import com.tissue.domain.AuthenticationProvider;
import com.tissue.domain.exception.AuthenticationErrorCode;
import com.tissue.domain.exception.EmailIdentityNotFoundException;
import com.tissue.feature.member.application.service.MemberFinder;
import com.tissue.feature.member.domain.Member;
import com.tissue.shared.exception.base.BadRequestException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService implements PasswordResetUseCase {

    private final MemberFinder memberFinder;
    private final AuthenticationIdentityRepository identityRepository;
    private final MemberEmailVerificationService emailVerificationService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public String requestPasswordReset(String email) {
        String verificationId = UUID.randomUUID().toString();

        boolean notExists = !identityRepository.existsByProviderAndIdentifier(AuthenticationProvider.EMAIL, email);
        if (notExists) {
            log.info("Password reset requested for non-existent email: {}", email);
            return verificationId;
        }

        return emailVerificationService.sendPasswordResetVerificationEmail(email);
    }

    @Override
    public boolean verifyEmailToken(String emailToken) {
        return emailVerificationService.verifyEmail(emailToken);
    }

    @Override
    public VerificationStatus getVerificationStatus(String verificationId) {
        return emailVerificationService.getVerificationStatus(verificationId);
    }

    @Override
    @Transactional
    public void resetPassword(String email, String resetToken, String newPassword) {
        if (!emailVerificationService.isTokenVerified(email, resetToken)) {
            throw new BadRequestException(AuthenticationErrorCode.INVALID_PASSWORD_RESET_TOKEN);
        }

        Member member =
                memberFinder.getActiveByEmail(email).orElseThrow(() -> new EmailIdentityNotFoundException(0L, email));

        AuthenticationIdentity identity = identityRepository
                .findByMemberIdAndProvider(member.getId(), AuthenticationProvider.EMAIL)
                .orElseThrow(() -> new EmailIdentityNotFoundException(member.getId(), email));

        identity.updateCredential(passwordEncoder.encode(newPassword));

        log.info("Password successfully reset for member: {}", member.getId());
    }
}
