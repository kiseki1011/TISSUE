package com.tissue.application.service;

import com.tissue.application.port.repository.EmailVerificationRepository;
import com.tissue.application.port.repository.EmailVerificationRepository.VerificationStatus;
import com.tissue.feature.member.config.EmailVerificationProperties;
import com.tissue.feature.member.domain.event.VerificationEmailRequestedEvent;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberEmailVerificationService {

    public static final String SIGNUP_VERIFY_URI = "/api/v1/members/signup/verify";
    public static final String PASSWORD_RESET_VERIFY_URI = "/api/v1/members/password/verify";

    private final EmailVerificationProperties properties;
    private final EmailVerificationRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public String sendSignupVerificationEmail(String email) {
        return sendVerificationEmail(email, SIGNUP_VERIFY_URI);
    }

    public String sendPasswordResetVerificationEmail(String email) {
        return sendVerificationEmail(email, PASSWORD_RESET_VERIFY_URI);
    }

    private String sendVerificationEmail(String email, String verifyUri) {
        String verificationId = UUID.randomUUID().toString();
        String emailToken = UUID.randomUUID().toString();

        repository.storeVerificationContext(verificationId, email, emailToken, properties.getEmailTtl());

        String link = "%s%s?token=%s".formatted(properties.getBaseUrl(), verifyUri, emailToken);
        eventPublisher.publishEvent(VerificationEmailRequestedEvent.create(email, link));

        return verificationId;
    }

    public boolean verifyEmail(String emailToken) {
        return repository.verifyByEmailToken(emailToken, properties.getVerifiedTokenTtl());
    }

    public VerificationStatus getVerificationStatus(String verificationId) {
        return repository.getStatus(verificationId);
    }

    public boolean isTokenVerified(String email, String token) {
        String verifiedEmail = repository.validateVerifiedToken(token);
        return Objects.equals(email, verifiedEmail);
    }
}
