package com.tissue.notification.scheduler;

import com.tissue.notification.application.port.in.EmailRetryUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailRetryScheduler {

    private final EmailRetryUseCase emailRetryUseCase;

    @Scheduled(fixedDelayString = "${tissue.notification.email.retry-interval-ms:180000}")
    public void retryFailedEmails() {
        emailRetryUseCase.retryFailedEmails();
    }
}
