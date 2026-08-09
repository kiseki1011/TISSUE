package com.tissue.feature.vcs.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Runs one delivery through its dispatcher and records the outcome. Deliberately not transactional: the
 * attempt and the failure bookkeeping must commit independently, so a failed attempt does not roll back
 * the record of having failed.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VcsWebhookProcessor {

    private final VcsWebhookDeliveryWriter writer;

    /**
     * Entry point for freshly received deliveries. Called from outside the bean so the async proxy applies,
     * which is what lets the webhook request return before any work is done.
     */
    @Async
    public void processAsync(Long deliveryId) {
        process(deliveryId);
    }

    public void process(Long deliveryId) {
        try {
            writer.attempt(deliveryId);
        } catch (Exception e) {
            writer.recordFailure(deliveryId, e);
        }
    }
}
