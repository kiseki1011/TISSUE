package com.tissue.application.service;

import com.tissue.application.port.repository.AuthenticationIdentityRepository;
import com.tissue.application.port.repository.EmailVerificationRepository;
import com.tissue.application.port.repository.EmailVerificationRepository.VerificationStatus;
import com.tissue.application.port.usecase.PasswordResetUseCase;
import com.tissue.domain.AuthenticationIdentity;
import com.tissue.domain.AuthenticationProvider;
import com.tissue.domain.exception.AuthenticationErrorCode;
import com.tissue.domain.exception.EmailIdentityNotFoundException;
import com.tissue.feature.member.application.service.MemberFinder;
import com.tissue.feature.member.config.EmailVerificationProperties;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.notification.application.port.repository.NotificationTemplateRenderer;
import com.tissue.shared.exception.base.BadRequestException;
import com.tissue.support.email.EmailClient;
import java.util.Map;
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

    private static final String VERIFY_PATH = "/api/v1/members/password/verify";

    private final MemberFinder memberFinder;
    private final AuthenticationIdentityRepository identityRepository;
    private final EmailVerificationRepository verificationRepository;
    private final EmailClient emailClient;
    private final NotificationTemplateRenderer templateRenderer;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationProperties properties;

    @Override
    @Transactional(readOnly = true)
    public String requestPasswordReset(String email) {
        String verificationId = UUID.randomUUID().toString();
        String emailToken = UUID.randomUUID().toString();

        Member member = memberFinder.getActiveByEmail(email).orElse(null);
        if (member == null) {
            log.info("Password reset requested for non-existent email: {}", email);
            return verificationId;
        }

        var ignored = identityRepository
                .findByMemberIdAndProvider(member.getId(), AuthenticationProvider.EMAIL)
                .orElseThrow(() -> new EmailIdentityNotFoundException(member.getId(), email));

        verificationRepository.storeVerificationContext(
                verificationId, email, emailToken, properties.getPasswordResetCodeTtl());

        sendResetEmail(email, emailToken);

        return verificationId;
    }

    @Override
    public void verifyEmailToken(String emailToken) {
        boolean success = verificationRepository.verifyByEmailToken(emailToken, properties.getPasswordResetTokenTtl());
        if (!success) {
            throw new BadRequestException(AuthenticationErrorCode.INVALID_PASSWORD_RESET_TOKEN);
        }
    }

    @Override
    public VerificationStatus getVerificationStatus(String verificationId) {
        return verificationRepository.getStatus(verificationId);
    }

    @Override
    @Transactional
    public void resetPassword(String resetToken, String newPassword) {
        String email = verificationRepository.validateVerifiedToken(resetToken);
        if (email == null) {
            throw new BadRequestException(AuthenticationErrorCode.INVALID_PASSWORD_RESET_TOKEN);
        }

        Member member =
                memberFinder.getActiveByEmail(email).orElseThrow(() -> new EmailIdentityNotFoundException(0L, email));

        AuthenticationIdentity identity = identityRepository
                .findByMemberIdAndProvider(member.getId(), AuthenticationProvider.EMAIL)
                .orElseThrow(() -> new EmailIdentityNotFoundException(member.getId(), email));

        identity.updateCredential(passwordEncoder.encode(newPassword));
        identityRepository.save(identity);

        log.info("Password successfully reset for member: {}", member.getId());
    }

    private void sendResetEmail(String to, String emailToken) {
        String subject = "[Tissue] Reset your password";
        String link = "%s%s?token=%s".formatted(properties.getBaseUrl(), VERIFY_PATH, emailToken);
        String body = templateRenderer.renderHtml("mail/password-reset-email", Map.of("resetLink", link));
        emailClient.send(to, subject, body);
    }
}
