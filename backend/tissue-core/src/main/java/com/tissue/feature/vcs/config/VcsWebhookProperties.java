package com.tissue.feature.vcs.config;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "tissue.vcs.webhook")
public class VcsWebhookProperties {

    /** Total attempts before a delivery is parked as dead. */
    private int maxAttempts = 5;

    /** Base delay for the first retry; doubles with each further attempt. */
    private Duration retryBackoff = Duration.ofMinutes(1);

    /** Upper bound on the doubling, so a delivery never waits absurdly long. */
    private Duration maxRetryBackoff = Duration.ofHours(1);

    /** How many due deliveries one retry sweep picks up. */
    private int retryBatchSize = 50;

    /** How long processed deliveries are kept as history before being purged. */
    private Duration retention = Duration.ofDays(30);
}
