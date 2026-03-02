package com.tissue.adapter.scheduler;

import com.tissue.adapter.persistence.EmailVerificationJpaRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@ConditionalOnProperty(name = "tissue.email.verification.token.store", havingValue = "rdb", matchIfMissing = true)
@RequiredArgsConstructor
public class ExpiredVerificationTokenCleaner {

    private final EmailVerificationJpaRepository tokenRepository;

    @Transactional
    @Scheduled(cron = "${tissue.email.verification.cleanup-cron:0 0 0 * * SUN}")
    public void cleanExpiredTokens() {
        log.info("[SCHEDULER] Starting cleanup of expired email verification tokens.");

        Instant now = Instant.now();
        tokenRepository.deleteByExpiresAtBefore(now);

        log.info("[SCHEDULER] Expired email verification tokens cleanup completed.");
    }
}
