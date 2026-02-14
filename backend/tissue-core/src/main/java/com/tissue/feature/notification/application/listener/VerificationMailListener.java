package com.tissue.feature.notification.application.listener;

import com.tissue.feature.member.domain.event.VerificationEmailRequestedEvent;
import com.tissue.feature.notification.application.port.usecase.SendVerificationEmailUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class VerificationMailListener {

    private final SendVerificationEmailUseCase verificationEmailUseCase;

    @Async
    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    @EventListener
    public void handleVerificationEmailRequest(VerificationEmailRequestedEvent event) {
        verificationEmailUseCase.sendVerificationEmail(event);
    }

    @Recover
    public void recover(Exception e, VerificationEmailRequestedEvent event) {
        log.error(
                "[EMAIL_VERIFICATION_FAILED] Failed to send email to {} after retries. Error: {}",
                event.email(),
                e.getMessage());
    }
}
