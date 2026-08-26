package com.tissue.feature.notification.domain.event;

import com.tissue.feature.notification.domain.enums.NotificationType;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record NotificationCreatedEvent(
        UUID eventId,
        NotificationType type,
        Set<Long> receiverMemberIds,
        @Nullable String projectKey,
        @Nullable String issueKey,
        @Nullable Long actorMemberId) {}
