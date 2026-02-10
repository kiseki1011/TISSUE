package com.tissue.notification.application.service;

import com.tissue.global.email.port.out.EmailClient;
import com.tissue.member.domain.event.VerificationEmailRequestedEvent;
import com.tissue.notification.application.port.in.SendVerificationEmailUseCase;
import com.tissue.notification.application.port.out.NotificationTemplateRenderer;
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
        log.info("[EMAIL_VERIFICATION_ATTEMPT] Sending email to {}", event.email());

        String subject = "Verify your email - Tissue";

        String content = templateRenderer.renderHtml(
                "mail/verification-email", Map.of("verificationLink", event.verificationLink()));

        emailClient.send(event.email(), subject, content);
        log.info("[EMAIL_VERIFICATION_SUCCESS] Sent email to {}", event.email());
    }
}
