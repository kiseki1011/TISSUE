package com.tissue.member.application.service;

import com.tissue.email.domain.EmailClient;
import com.tissue.member.adapter.in.web.config.EmailVerificationProperties;
import com.tissue.member.application.port.out.EmailVerificationRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberEmailVerificationService {

    private final EmailClient emailClient;
    private final EmailVerificationProperties properties;
    private final EmailVerificationRepository repository;

    public void sendVerificationEmail(String email) {
        String tokenValue = UUID.randomUUID().toString();
        repository.saveToken(email, tokenValue, properties.getTtl());

        String link = properties.getVerificationUrl() + "?email=%s&token=%s".formatted(email, tokenValue);

        String subject = "Tissue - Email Verification";
        String content = """
                Hello,

                Please verify your email address by clicking the link below:

                %s

                This link is valid for 30 minutes.

                - Tissue Team
                """.formatted(link);

        emailClient.send(email, subject, content);
    }

    public boolean verifyEmail(String email, String tokenValue) {
        return repository.verify(email, tokenValue);
    }

    public boolean isEmailVerified(String email) {
        return repository.isVerified(email);
    }

    public void clearVerification(String email) {
        repository.deleteToken(email);
    }
}
