package com.tissue.application.service;

import com.tissue.application.port.repository.AuthenticationIdentityRepository;
import com.tissue.application.port.repository.EmailVerificationRepository;
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
import java.security.SecureRandom;
import java.util.Map;
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
    private final EmailVerificationRepository verificationRepository;
    private final EmailClient emailClient;
    private final NotificationTemplateRenderer templateRenderer;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationProperties properties;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    @Transactional(readOnly = true)
    public void requestPasswordReset(String email) {
        Member member = memberFinder.getActiveByEmail(email).orElse(null);
        if (member == null) {
            log.info("Password reset requested for non-existent email: {}", email);
            return;
        }

        var ignored = identityRepository
                .findByMemberIdAndProvider(member.getId(), AuthenticationProvider.EMAIL)
                .orElseThrow(() -> new EmailIdentityNotFoundException(member.getId(), email));

        String code = generateCode();
        verificationRepository.storeResetCode(email, code, properties.getPasswordResetCodeTtl());

        sendResetEmail(email, code);
    }

    @Override
    public String verifyResetCode(String email, String code) {
        String resetToken = verificationRepository.verifyResetCode(email, code, properties.getPasswordResetTokenTtl());
        if (resetToken == null) {
            throw new BadRequestException(AuthenticationErrorCode.INVALID_PASSWORD_RESET_CODE);
        }
        return resetToken;
    }

    @Override
    @Transactional
    public void resetPassword(String resetToken, String newPassword) {
        String email = verificationRepository.validateResetToken(resetToken);
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

    private String generateCode() {
        return String.format("%06d", RANDOM.nextInt(1000000));
    }

    private void sendResetEmail(String to, String code) {
        String subject = "[Tissue] Reset your password";
        String body = templateRenderer.renderHtml("mail/password-reset-email", Map.of("code", code));
        emailClient.send(to, subject, body);
    }
}
