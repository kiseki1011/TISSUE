package com.tissue.notification.presentation.dto.response;

import com.tissue.common.vo.EntityReference;
import com.tissue.notification.domain.enums.NotificationType;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record NotificationResponse(
        Long id,
        UUID eventId,
        NotificationType type,
        String title,
        String content,
        EntityReference entityReference,
        Long actorMemberId,
        String actorDisplayName,
        boolean isRead,
        Instant createdAt) {}
