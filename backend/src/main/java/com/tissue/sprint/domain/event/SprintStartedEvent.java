package com.tissue.sprint.domain.event;

import com.tissue.common.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record SprintStartedEvent(
        UUID eventId,
        Instant occurredAt,
        String workspaceKey,
        String projectKey,
        Long sprintId,
        String sprintTitle,
        Long actorMemberId,
        String actorDisplayName)
        implements DomainEvent {

    public static SprintStartedEvent create(
            String workspaceKey,
            String projectKey,
            Long sprintId,
            String sprintTitle,
            Long actorMemberId,
            String actorDisplayName) {
        return new SprintStartedEvent(
                UUID.randomUUID(),
                Instant.now(),
                workspaceKey,
                projectKey,
                sprintId,
                sprintTitle,
                actorMemberId,
                actorDisplayName);
    }
}
