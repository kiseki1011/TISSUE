package com.tissue.feature.notification.application.service;

import com.tissue.feature.member.domain.event.VerificationEmailRequestedEvent;
import com.tissue.feature.notification.application.port.repository.NotificationTemplateRenderer;
import com.tissue.feature.notification.application.port.usecase.SendVerificationEmailUseCase;
import com.tissue.global.email.EmailClient;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberVerificationEmailService implements SendVerificationEmailUseCase {

    private final EmailClient emailClient;
    private final NotificationTemplateRenderer templateRenderer;

    @Override
    public void sendVerificationEmail(VerificationEmailRequestedEvent event) {
        log.info("Sending verification email to {}", event.email());

        String subject = "Verify your email - Tissue";

        String content = templateRenderer.renderHtml(
                "mail/verification-email", Map.of("verificationLink", event.verificationLink()));

        emailClient.send(event.email(), subject, content);
        log.info("Verification email sent to {}", event.email());
    }
}
