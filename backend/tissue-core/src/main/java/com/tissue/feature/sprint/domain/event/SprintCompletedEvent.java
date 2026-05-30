package com.tissue.feature.sprint.domain.event;

import com.tissue.shared.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record SprintCompletedEvent(
        UUID eventId,
        Instant occurredAt,
        String projectKey,
        Long sprintId,
        String sprintTitle,
        @Nullable Instant startedAt,
        @Nullable Instant endedAt,
        Long actorMemberId,
        String actorDisplayName)
        implements DomainEvent {

    public static SprintCompletedEvent create(
            String projectKey,
            Long sprintId,
            String sprintTitle,
            @Nullable Instant startedAt,
            @Nullable Instant endedAt,
            Long actorMemberId,
            String actorDisplayName) {
        return new SprintCompletedEvent(
                UUID.randomUUID(),
                Instant.now(),
                projectKey,
                sprintId,
                sprintTitle,
                startedAt,
                endedAt,
                actorMemberId,
                actorDisplayName);
    }
}
