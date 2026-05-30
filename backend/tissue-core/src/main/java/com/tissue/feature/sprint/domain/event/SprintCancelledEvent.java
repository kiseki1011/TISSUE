package com.tissue.feature.sprint.domain.event;

import com.tissue.shared.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record SprintCancelledEvent(
        UUID eventId,
        Instant occurredAt,
        String projectKey,
        Long sprintId,
        String sprintTitle,
        Long actorMemberId,
        String actorDisplayName)
        implements DomainEvent {

    public static SprintCancelledEvent create(
            String projectKey, Long sprintId, String title, Long actorId, String actorName) {
        return new SprintCancelledEvent(
                UUID.randomUUID(), Instant.now(), projectKey, sprintId, title, actorId, actorName);
    }
}
