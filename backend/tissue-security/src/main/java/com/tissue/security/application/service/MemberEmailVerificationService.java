package com.tissue.security.application.service;

import com.tissue.feature.member.domain.event.VerificationEmailRequestedEvent;
import com.tissue.security.application.port.repository.EmailVerificationRepository;
import com.tissue.security.application.port.repository.EmailVerificationRepository.VerificationStatus;
import com.tissue.security.config.EmailVerificationProperties;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberEmailVerificationService {

    private final EmailVerificationProperties properties;
    private final EmailVerificationRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    private final RateLimitService rateLimitService;

    @Transactional
    public String sendSignupVerificationEmail(String email) {
        rateLimitService.checkEmailVerificationRateLimit(email);
        return sendVerificationEmail(email, properties.getSignupVerifyPath());
    }

    @Transactional
    public String sendPasswordResetVerificationEmail(String email) {
        return sendVerificationEmail(email, properties.getPasswordResetVerifyPath());
    }

    @Transactional
    public boolean verifyEmail(String emailToken) {
        return repository.verifyByEmailToken(emailToken, properties.getVerifiedTokenTtl());
    }

    @Transactional(readOnly = true)
    public VerificationStatus getVerificationStatus(String verificationId) {
        return repository.getStatus(verificationId);
    }

    public boolean isTokenVerified(String email, String token) {
        String verifiedEmail = repository.validateVerifiedToken(token);
        return Objects.equals(email, verifiedEmail);
    }

    private String sendVerificationEmail(String email, String verifyUri) {
        String verificationId = UUID.randomUUID().toString();
        String emailToken = UUID.randomUUID().toString();

        repository.storeVerificationContext(verificationId, email, emailToken, properties.getEmailTtl());

        String link = "%s%s?token=%s".formatted(properties.getBaseUrl(), verifyUri, emailToken);
        eventPublisher.publishEvent(VerificationEmailRequestedEvent.create(email, link));

        return verificationId;
    }
}
