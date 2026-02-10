package com.tissue.notification.application.service;

import com.tissue.global.email.port.out.EmailClient;
import com.tissue.notification.application.port.in.EmailRetryUseCase;
import com.tissue.notification.application.port.out.FailedEmailRepository;
import com.tissue.notification.domain.FailedEmail;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailRetryService implements EmailRetryUseCase {

    private final FailedEmailRepository failedEmailRepository;
    private final EmailClient emailClient;

    @Override
    @Transactional
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
