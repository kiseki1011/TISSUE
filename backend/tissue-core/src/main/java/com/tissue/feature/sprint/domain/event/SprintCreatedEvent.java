package com.tissue.feature.sprint.domain.event;

import com.tissue.shared.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record SprintCreatedEvent(
        UUID eventId,
        Instant occurredAt,
        String workspaceKey,
        String projectKey,
        Long sprintId,
        String sprintTitle,
        Long actorMemberId,
        String actorDisplayName)
        implements DomainEvent {

    public static SprintCreatedEvent create(
            String workspaceKey, String projectKey, Long sprintId, String title, Long actorId, String actorName) {

        return new SprintCreatedEvent(
                UUID.randomUUID(), Instant.now(), workspaceKey, projectKey, sprintId, title, actorId, actorName);
    }
}
