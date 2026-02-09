package com.tissue.notification.scheduler;

import com.tissue.global.email.domain.EmailClient;
import com.tissue.notification.application.port.out.FailedEmailRepository;
import com.tissue.notification.domain.FailedEmail;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailRetryScheduler {

    private final FailedEmailRepository failedEmailRepository;
    private final EmailClient emailClient;

    // TODO: Consider separating the logic to EmailRetryService and just calling the service.
    @Transactional
    @Scheduled(fixedDelayString = "${tissue.notification.email.retry-interval-ms:180000}")
    public void retryFailedEmails() {
        List<FailedEmail> targets = failedEmailRepository.findAllByNextRetryAtBefore(LocalDateTime.now());

        if (targets.isEmpty()) {
            return;
        }

        log.info("Retrying {} failed emails...", targets.size());

        for (FailedEmail failedEmail : targets) {
            try {
                emailClient.send(failedEmail.getReceiverEmail(), failedEmail.getSubject(), failedEmail.getBody());
                failedEmailRepository.delete(failedEmail);
                log.info("Successfully resent email to {}", failedEmail.getReceiverEmail());
            } catch (Exception e) {
                log.warn("Retry failed for email {}: {}", failedEmail.getId(), e.getMessage());
                failedEmail.incrementRetryCount();
                failedEmailRepository.save(failedEmail);
            }
        }
    }
}
