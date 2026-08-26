package com.tissue.feature.vcs.application.dto.response;

import com.tissue.feature.vcs.domain.VcsWebhookDelivery;
import com.tissue.feature.vcs.domain.enums.WebhookDeliveryStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * One inbound webhook delivery, as an operator sees it. Deliberately omits the stored payload: it is large,
 * it is only needed to replay a delivery internally, and it carries repository content that has no place in
 * a status list.
 */
@Schema(description = "An inbound webhook delivery and how it was handled.")
public record VcsWebhookDeliverySummary(
        @Schema(description = "Internal id of this delivery record")
        Long id,

        @Schema(description = "The provider's own delivery id, matching its delivery log")
        String deliveryId,

        @Schema(description = "Provider event type, e.g. push or pull_request")
        String eventType,

        @Schema(description = "How the delivery ended, or that it is still being retried")
        WebhookDeliveryStatus status,

        @Schema(description = "Why it ended that way, e.g. which branch linked or why nothing was done") @Nullable
        String resultDetail,

        @Schema(description = "Failure reason, when the delivery could not be processed") @Nullable
        String lastError,

        @Schema(description = "How many attempts have failed so far")
        int attemptCount,

        @Schema(description = "When the delivery arrived") Instant receivedAt,

        @Schema(description = "When it reached a final state") @Nullable
        Instant processedAt,

        @Schema(description = "When the next retry is due, while it is still being retried") @Nullable
        Instant nextAttemptAt) {

    public static VcsWebhookDeliverySummary from(VcsWebhookDelivery delivery) {
        return new VcsWebhookDeliverySummary(
                delivery.getId(),
                delivery.getDeliveryId(),
                delivery.getEventType(),
                delivery.getStatus(),
                delivery.getResultDetail(),
                delivery.getLastError(),
                delivery.getAttemptCount(),
                delivery.getCreatedAt(),
                delivery.getProcessedAt(),
                delivery.getNextAttemptAt());
    }
}
