package com.tissue.feature.notification.application.dto.response;

import com.tissue.feature.notification.domain.enums.NotificationType;
import com.tissue.shared.vo.EntityReference;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Schema(description = "A notification sent to a member about an event.")
@Builder
public record NotificationResponse(
        Long id,

        @Schema(example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        UUID eventId,

        @Schema(example = "ISSUE_ASSIGNED") NotificationType type,
        Map<String, String> data,
        EntityReference entityReference,
        @Nullable Long actorMemberId,
        @Nullable String actorDisplayName,
        boolean isRead,
        Instant createdAt) {}
