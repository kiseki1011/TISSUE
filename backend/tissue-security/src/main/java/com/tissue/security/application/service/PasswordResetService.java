package com.tissue.security.application.service;

import com.tissue.feature.member.application.service.MemberFinder;
import com.tissue.feature.member.domain.Member;
import com.tissue.security.application.port.repository.AuthenticationIdentityRepository;
import com.tissue.security.application.port.repository.EmailVerificationRepository.VerificationStatus;
import com.tissue.security.application.port.repository.RefreshTokenRepository;
import com.tissue.security.application.port.usecase.PasswordResetUseCase;
import com.tissue.security.domain.AuthenticationIdentity;
import com.tissue.security.domain.AuthenticationIdentityProvider;
import com.tissue.security.domain.exception.AuthenticationErrorCode;
import com.tissue.security.domain.exception.EmailIdentityNotFoundException;
import com.tissue.security.util.MaskingUtil;
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
    private final RefreshTokenRepository refreshTokenRepository;
    private final RateLimitService rateLimitService;

    @Override
    public String requestPasswordReset(String email) {
        rateLimitService.checkPasswordResetRateLimit(email);
        String verificationId = UUID.randomUUID().toString();

        boolean notExists =
                !identityRepository.existsByProviderAndIdentifier(AuthenticationIdentityProvider.EMAIL, email);
        if (notExists) {
            log.info("Password reset requested for non-existent email: {}", MaskingUtil.maskEmail(email));
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
                .findByMemberIdAndProvider(member.getId(), AuthenticationIdentityProvider.EMAIL)
                .orElseThrow(() -> new EmailIdentityNotFoundException(member.getId(), email));

        identity.updateCredential(passwordEncoder.encode(newPassword));

        refreshTokenRepository.deleteByEmail(email);

        log.info("Password successfully reset for member: {}", member.getId());
    }
}
