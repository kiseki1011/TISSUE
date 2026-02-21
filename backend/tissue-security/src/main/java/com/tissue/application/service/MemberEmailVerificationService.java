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

    // TODO: 설정값으로 관리하거나, 따로 관리하는게 좋을 것 같은데
    private static final String VERIFY_PATH = "/api/v1/members/verification/verify";

    private final EmailVerificationProperties properties;
    private final EmailVerificationRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Starts the verification process.
     *
     * @param email The email to verify.
     * @return The verificationId for the client to poll.
     */
    public String sendVerificationEmail(String email) {
        String verificationId = UUID.randomUUID().toString();
        String emailToken = UUID.randomUUID().toString();

        repository.storeVerificationContext(verificationId, email, emailToken, properties.getVerificationEmailTtl());
        String link = "%s%s?token=%s".formatted(properties.getBaseUrl(), VERIFY_PATH, emailToken);

        eventPublisher.publishEvent(VerificationEmailRequestedEvent.create(email, link));

        return verificationId;
    }

    public boolean verifyEmail(String token) {
        return repository.verifyByEmailToken(token, properties.getSignupTokenTtl());
    }

    public VerificationStatus getVerificationStatus(String verificationId) {
        return repository.getStatus(verificationId);
    }

    public boolean validateSignupToken(String email, String signupToken) {
        String verifiedEmail = repository.validateVerifiedToken(signupToken);
        return Objects.equals(email, verifiedEmail);
    }
}
