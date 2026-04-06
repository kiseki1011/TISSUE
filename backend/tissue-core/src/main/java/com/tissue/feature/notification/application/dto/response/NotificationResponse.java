package com.tissue.feature.notification.application.dto.response;

import com.tissue.feature.notification.domain.enums.NotificationType;
import com.tissue.shared.vo.EntityReference;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Schema(description = "A notification sent to a workspace member about an event.")
@Builder
public record NotificationResponse(
        @Schema(example = "1") Long id,

        @Schema(example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        UUID eventId,

        @Schema(example = "ISSUE_ASSIGNED") NotificationType type,
        Map<String, String> data,
        EntityReference entityReference,
        @Schema(example = "123") @Nullable Long actorMemberId,
        @Schema(example = "Gildong Hong") @Nullable String actorDisplayName,
        @Schema(example = "false") boolean isRead,
        Instant createdAt) {}
