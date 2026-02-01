package com.tissue.notification.adapter.event;

import com.tissue.global.email.domain.EmailClient;
import com.tissue.member.domain.event.VerificationEmailRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Slf4j
@Component
@RequiredArgsConstructor
public class VerificationMailListener {

    private final EmailClient emailClient;
    private final SpringTemplateEngine templateEngine;

    @Async
    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleVerificationEmailRequest(VerificationEmailRequestedEvent event) {
        log.info("[EMAIL_VERIFICATION_ATTEMPT] Sending email to {}", event.email());

        String subject = "Verify your email - Tissue";

        Context context = new Context();
        context.setVariable("verificationLink", event.verificationLink());

        String content = templateEngine.process("mail/verification-email", context);

        emailClient.send(event.email(), subject, content);
        log.info("[EMAIL_VERIFICATION_SUCCESS] Sent email to {}", event.email());
    }

    @Recover
    public void recover(Exception e, VerificationEmailRequestedEvent event) {
        log.error(
                "[EMAIL_VERIFICATION_FAILED] Failed to send email to {} after retries. Error: {}",
                event.email(),
                e.getMessage());
    }
}
