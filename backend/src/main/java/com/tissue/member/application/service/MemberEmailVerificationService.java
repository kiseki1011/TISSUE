package com.tissue.member.application.service;

import com.tissue.email.domain.EmailClient;
import com.tissue.member.adapter.in.web.config.EmailVerificationProperties;
import com.tissue.member.application.port.out.EmailVerificationRepository;
import com.tissue.member.application.port.out.EmailVerificationRepository.VerificationStatus;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberEmailVerificationService {

    private final EmailClient emailClient;
    private final EmailVerificationProperties properties;
    private final EmailVerificationRepository repository;

    /**
     * Starts the verification process.
     *
     * @param email The email to verify.
     * @return The verificationId (secure request ID) for the client to poll.
     */
    public String sendVerificationEmail(String email) {
        String emailToken = UUID.randomUUID().toString();

        // Start verification flow and get the secure verificationId
        String verificationId = repository.startVerification(email, emailToken, properties.getTtl());

        // Link contains only the emailToken, NOT the verificationId
        String link = properties.getVerificationUrl() + "?token=%s".formatted(emailToken);

        String subject = "Tissue - Email Verification";
        String content = """
                Hello,

                Please verify your email address by clicking the link below:

                %s

                This link is valid for 10 minutes.

                - Tissue Team
                """.formatted(link);

        emailClient.send(email, subject, content);

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
