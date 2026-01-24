package com.tissue.notification.application.dto.response;

import com.tissue.common.vo.EntityReference;
import com.tissue.notification.domain.enums.NotificationType;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Builder
public record NotificationResponse(
        Long id,
        UUID eventId,
        NotificationType type,
        String title,
        String content,
        EntityReference entityReference,
        @Nullable Long actorMemberId,
        @Nullable String actorDisplayName,
        boolean isRead,
        Instant createdAt) {}
