package com.tissue.security.adapter.scheduler;

import com.tissue.security.adapter.persistence.RefreshTokenJpaRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@ConditionalOnProperty(name = "tissue.use-redis", havingValue = "false", matchIfMissing = true)
@RequiredArgsConstructor
public class ExpiredRefreshTokenCleaner {

    private final RefreshTokenJpaRepository tokenRepository;

    @Transactional
    @Scheduled(cron = "${tissue.security.refresh-token.cleanup-cron:0 0 0 * * SUN}")
    public void cleanExpiredTokens() {
        log.info("Starting cleanup of expired refresh tokens");

        Instant now = Instant.now();
        tokenRepository.deleteByExpiresAtBefore(now);

        log.info("Expired refresh tokens cleanup completed");
    }
}
