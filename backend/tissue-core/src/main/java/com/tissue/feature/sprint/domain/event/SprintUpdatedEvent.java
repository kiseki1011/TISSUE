package com.tissue.feature.sprint.domain.event;

import com.tissue.shared.dto.FieldChange;
import com.tissue.shared.event.DomainEvent;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record SprintUpdatedEvent(
        UUID eventId,
        Instant occurredAt,
        String projectKey,
        Long sprintId,
        String sprintTitle,
        Map<String, FieldChange> changes,
        Long actorMemberId,
        String actorDisplayName)
        implements DomainEvent {

    public static SprintUpdatedEvent create(
            String projectKey,
            Long sprintId,
            String title,
            Map<String, FieldChange> changes,
            Long actorId,
            String actorName) {
        return new SprintUpdatedEvent(
                UUID.randomUUID(), Instant.now(), projectKey, sprintId, title, changes, actorId, actorName);
    }
}
