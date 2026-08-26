package com.tissue.feature.vcs.application.service;

import com.tissue.feature.vcs.application.dto.VcsEventResult;
import com.tissue.feature.vcs.application.port.repository.VcsWebhookDeliveryRepository;
import com.tissue.feature.vcs.application.port.usecase.VcsWebhookDispatcher;
import com.tissue.feature.vcs.config.VcsWebhookProperties;
import com.tissue.feature.vcs.domain.VcsWebhookDelivery;
import com.tissue.feature.vcs.domain.enums.VcsProvider;
import com.tissue.feature.vcs.domain.enums.WebhookDeliveryStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The transactional units of the webhook inbox, kept apart from the orchestration in
 * {@link VcsWebhookProcessor}: an attempt that fails marks its own transaction rollback-only, so the
 * failure must be recorded from outside it or the bookkeeping would roll back with the work.
 */
@Slf4j
@Service
public class VcsWebhookDeliveryWriter {

    private final VcsWebhookDeliveryRepository repository;
    private final VcsWebhookProperties properties;
    private final Map<VcsProvider, VcsWebhookDispatcher> dispatchers;

    public VcsWebhookDeliveryWriter(
            VcsWebhookDeliveryRepository repository,
            VcsWebhookProperties properties,
            List<VcsWebhookDispatcher> dispatchers) {
        this.repository = repository;
        this.properties = properties;
        this.dispatchers = dispatchers.stream()
                .collect(Collectors.toMap(
                        VcsWebhookDispatcher::provider,
                        Function.identity(),
                        (a, b) -> a,
                        () -> new EnumMap<>(VcsProvider.class)));
    }

    /**
     * Persists a newly received delivery. Throws {@code DataIntegrityViolationException} when the
     * provider's delivery id was already stored, which is how redeliveries are rejected.
     */
    @Transactional
    public Long create(VcsProvider provider, String deliveryId, String projectKey, String eventType, String payload) {
        VcsWebhookDelivery delivery =
                repository.save(VcsWebhookDelivery.create(provider, deliveryId, projectKey, eventType, payload));

        return delivery.getId();
    }

    @Transactional
    public void attempt(Long deliveryId) {
        VcsWebhookDelivery delivery = repository.findById(deliveryId).orElse(null);
        if (delivery == null) {
            log.warn("Webhook delivery {} disappeared before it could be processed", deliveryId);
            return;
        }
        if (delivery.isTerminal()) {
            return;
        }

        VcsWebhookDispatcher dispatcher = dispatchers.get(delivery.getProvider());
        if (dispatcher == null) {
            delivery.markIgnored("No dispatcher registered for provider " + delivery.getProvider());
            return;
        }

        VcsEventResult result =
                dispatcher.dispatch(delivery.getProjectKey(), delivery.getEventType(), delivery.getPayload());

        if (result.handled()) {
            delivery.markProcessed(result.detail());
        } else {
            delivery.markIgnored(result.detail());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(Long deliveryId, Throwable error) {
        VcsWebhookDelivery delivery = repository.findById(deliveryId).orElse(null);
        if (delivery == null) {
            return;
        }

        Instant nextAttemptAt = nextAttemptAt(delivery.getAttemptCount());
        delivery.recordFailure(describe(error), nextAttemptAt);

        if (nextAttemptAt == null) {
            log.error(
                    "Webhook delivery {} gave up after {} attempts. project={}, event={}",
                    deliveryId,
                    delivery.getAttemptCount(),
                    delivery.getProjectKey(),
                    delivery.getEventType(),
                    error);
        } else {
            log.warn(
                    "Webhook delivery {} failed (attempt {}), retrying at {}",
                    deliveryId,
                    delivery.getAttemptCount(),
                    nextAttemptAt,
                    error);
        }
    }

    @Transactional(readOnly = true)
    public List<Long> findDueForRetry(Instant now) {
        return repository.findDueForRetry(WebhookDeliveryStatus.FAILED, now, Limit.of(properties.getRetryBatchSize()));
    }

    @Transactional
    public int purgeOlderThan(Instant threshold) {
        return repository.deleteOlderThan(threshold);
    }

    /**
     * Exponential backoff on the attempt just spent. Null once the retry budget is exhausted, which tells
     * the delivery to park itself as DEAD.
     */
    @Nullable
    private Instant nextAttemptAt(int attemptsSoFar) {
        int attemptsAfterThisFailure = attemptsSoFar + 1;
        if (attemptsAfterThisFailure >= properties.getMaxAttempts()) {
            return null;
        }

        Duration backoff = properties.getRetryBackoff().multipliedBy(1L << Math.min(attemptsSoFar, 20));
        Duration capped =
                backoff.compareTo(properties.getMaxRetryBackoff()) > 0 ? properties.getMaxRetryBackoff() : backoff;

        return Instant.now().plus(capped);
    }

    private static String describe(Throwable error) {
        return error.getClass().getSimpleName() + ": " + error.getMessage();
    }
}
