package com.tissue.application.service;

import com.tissue.feature.member.application.port.out.EmailVerificationRepository;
import com.tissue.feature.member.application.port.out.EmailVerificationRepository.VerificationStatus;
import com.tissue.feature.member.config.EmailVerificationProperties;
import com.tissue.feature.member.domain.event.VerificationEmailRequestedEvent;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberEmailVerificationService {

    private final EmailVerificationProperties properties;
    private final EmailVerificationRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Starts the verification process.
     *
     * @param email The email to verify.
     * @return The verificationId (secure request ID) for the client to poll.
     */
    public String sendVerificationEmail(String email) {
        // TODO: 이메일 유니크 검증 필요

        String emailToken = UUID.randomUUID().toString();

        // start verification flow and get the secure verificationId
        String verificationId = repository.startVerification(email, emailToken, properties.getTtl());

        String link = properties.getVerificationUrl() + "?token=%s".formatted(emailToken);

        eventPublisher.publishEvent(VerificationEmailRequestedEvent.create(email, link));

        return verificationId;
    }

    public boolean verifyEmail(String token) {
        return repository.verifyByToken(token);
    }

    public VerificationStatus getVerificationStatus(String verificationId) {
        return repository.getStatus(verificationId);
    }

    public boolean validateSignupToken(String email, String signupToken) {
        return repository.validateSignupToken(email, signupToken);
    }
}
