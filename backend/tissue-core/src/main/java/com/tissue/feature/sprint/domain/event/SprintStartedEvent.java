package com.tissue.feature.sprint.domain.event;

import com.tissue.shared.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record SprintStartedEvent(
        UUID eventId,
        Instant occurredAt,
        String projectKey,
        Long sprintId,
        String sprintTitle,
        Long actorMemberId,
        String actorDisplayName)
        implements DomainEvent {

    public static SprintStartedEvent create(
            String projectKey, Long sprintId, String sprintTitle, Long actorMemberId, String actorDisplayName) {
        return new SprintStartedEvent(
                UUID.randomUUID(), Instant.now(), projectKey, sprintId, sprintTitle, actorMemberId, actorDisplayName);
    }
}
