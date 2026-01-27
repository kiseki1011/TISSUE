package com.tissue.member.application.service;

import com.tissue.email.domain.EmailClient;
import com.tissue.member.adapter.in.web.config.EmailVerificationProperties;
import com.tissue.member.application.port.out.EmailVerificationRepository;
import com.tissue.member.application.port.out.EmailVerificationRepository.VerificationStatus;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberEmailVerificationService {

    private final EmailClient emailClient;
    private final EmailVerificationProperties properties;
    private final EmailVerificationRepository repository;
    private final SpringTemplateEngine templateEngine;

    /**
     * Starts the verification process.
     *
     * @param email The email to verify.
     * @return The verificationId (secure request ID) for the client to poll.
     */
    public String sendVerificationEmail(String email) {
        String emailToken = UUID.randomUUID().toString();

        // start verification flow and get the secure verificationId
        String verificationId = repository.startVerification(email, emailToken, properties.getTtl());

        String link = properties.getVerificationUrl() + "?token=%s".formatted(emailToken);

        Context context = new Context();
        context.setVariable("verificationLink", link);

        // Explicitly using SpringTemplateEngine
        String content = templateEngine.process("mail/verification-email", context);

        String subject = "Verify your email - Tissue";

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
