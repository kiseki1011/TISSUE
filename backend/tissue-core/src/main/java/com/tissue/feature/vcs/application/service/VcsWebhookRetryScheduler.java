package com.tissue.feature.vcs.application.service;

import com.tissue.feature.vcs.config.VcsWebhookProperties;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Retries failed deliveries and prunes old ones.
 *
 * <p>Retrying is on us rather than on the provider: GitHub does not guarantee it will resend a delivery we
 * failed to process, so without this sweep a transient database blip would silently lose the event.
 *
 * <p>Assumes a single application instance. Running several would let two instances pick up the same due
 * delivery; the work is idempotent per attempt but would be duplicated, so this needs a claim (an
 * {@code UPDATE ... RETURNING} or {@code SKIP LOCKED}) before scaling out.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VcsWebhookRetryScheduler {

    private final VcsWebhookDeliveryWriter writer;
    private final VcsWebhookProcessor processor;
    private final VcsWebhookProperties properties;

    @Scheduled(fixedRateString = "${tissue.vcs.webhook.retry-scan-ms:60000}")
    public void retryFailedDeliveries() {
        List<Long> due = writer.findDueForRetry(Instant.now());
        if (due.isEmpty()) {
            return;
        }

        log.info("Retrying {} failed VCS webhook deliveries", due.size());
        due.forEach(processor::process);
    }

    @Scheduled(cron = "${tissue.vcs.webhook.purge-cron:0 30 3 * * *}")
    public void purgeExpiredDeliveries() {
        Instant threshold = Instant.now().minus(properties.getRetention());
        int purged = writer.purgeOlderThan(threshold);

        if (purged > 0) {
            log.info("Purged {} VCS webhook deliveries older than {}", purged, threshold);
        }
    }
}
