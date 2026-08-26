package com.tissue.feature.vcs.domain;

import com.tissue.feature.vcs.domain.enums.VcsProvider;
import com.tissue.feature.vcs.domain.enums.WebhookDeliveryStatus;
import com.tissue.shared.entity.BaseDateEntity;
import com.tissue.shared.meta.LLMGenerated;
import com.tissue.shared.meta.LLMInvolvement;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

/**
 * A single inbound webhook delivery, persisted before it is processed.
 *
 * <p>The provider's delivery id is unique, which is what makes redeliveries safe to receive: the insert
 * fails and the duplicate is dropped instead of replaying a transition. Keeping the raw payload lets a
 * failed delivery be retried later, since GitHub does not guarantee it will resend one for us. The row
 * doubles as the delivery history an operator needs to answer "why did my push not attach?".
 */
@Entity
@Getter
@LLMGenerated(llmInvolvement = LLMInvolvement.ASSISTED, model = "claude-opus-5")
@Table(
        name = "vcs_webhook_delivery",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_vcs_delivery_provider_delivery_id",
                        columnNames = {"vcs_provider", "delivery_id"}),
        indexes = {
            @Index(name = "idx_vcs_delivery_retry", columnList = "status, next_attempt_at"),
            @Index(name = "idx_vcs_delivery_project", columnList = "project_key, created_at")
        })
public class VcsWebhookDelivery extends BaseDateEntity {

    public static final int MAX_DETAIL_LENGTH = 500;
    public static final int MAX_ERROR_LENGTH = 1000;

    @Enumerated(EnumType.STRING)
    @Column(name = "vcs_provider", nullable = false)
    private VcsProvider provider;

    @Column(name = "delivery_id", nullable = false)
    private String deliveryId;

    @Column(name = "project_key", nullable = false)
    private String projectKey;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "text")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WebhookDeliveryStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Nullable
    @Column(name = "result_detail", length = MAX_DETAIL_LENGTH)
    private String resultDetail;

    @Nullable
    @Column(name = "last_error", length = MAX_ERROR_LENGTH)
    private String lastError;

    @Nullable
    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Nullable
    @Column(name = "processed_at")
    private Instant processedAt;

    @SuppressWarnings("NullAway.Init")
    protected VcsWebhookDelivery() {}

    public static VcsWebhookDelivery create(
            VcsProvider provider, String deliveryId, String projectKey, String eventType, String payload) {
        VcsWebhookDelivery delivery = new VcsWebhookDelivery();
        delivery.provider = provider;
        delivery.deliveryId = deliveryId;
        delivery.projectKey = projectKey;
        delivery.eventType = eventType;
        delivery.payload = payload;
        delivery.status = WebhookDeliveryStatus.RECEIVED;
        delivery.attemptCount = 0;
        return delivery;
    }

    public void markProcessed(String detail) {
        this.status = WebhookDeliveryStatus.PROCESSED;
        finish(detail);
    }

    public void markIgnored(String detail) {
        this.status = WebhookDeliveryStatus.IGNORED;
        finish(detail);
    }

    /**
     * Records a failed attempt. A null {@code nextAttemptAt} means the retry budget is spent, so the
     * delivery is parked as {@code DEAD} rather than being picked up again.
     */
    public void recordFailure(String error, @Nullable Instant nextAttemptAt) {
        this.attemptCount++;
        this.lastError = truncate(error, MAX_ERROR_LENGTH);
        this.nextAttemptAt = nextAttemptAt;
        this.status = (nextAttemptAt == null) ? WebhookDeliveryStatus.DEAD : WebhookDeliveryStatus.FAILED;
        if (nextAttemptAt == null) {
            this.processedAt = Instant.now();
        }
    }

    public boolean isTerminal() {
        return status == WebhookDeliveryStatus.PROCESSED
                || status == WebhookDeliveryStatus.IGNORED
                || status == WebhookDeliveryStatus.DEAD;
    }

    private void finish(String detail) {
        this.resultDetail = truncate(detail, MAX_DETAIL_LENGTH);
        this.lastError = null;
        this.nextAttemptAt = null;
        this.processedAt = Instant.now();
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}
