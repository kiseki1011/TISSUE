package com.tissue.notification.presentation.dto.response;

import com.tissue.notification.domain.enums.NotificationType;
import com.tissue.notification.domain.vo.EntityReference;
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
