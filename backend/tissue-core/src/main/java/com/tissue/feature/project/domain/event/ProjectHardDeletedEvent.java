package com.tissue.feature.project.domain.event;

import com.tissue.shared.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record ProjectHardDeletedEvent(UUID eventId, Instant occurredAt, String projectKey) implements DomainEvent {

    public static ProjectHardDeletedEvent create(String projectKey) {
        return new ProjectHardDeletedEvent(UUID.randomUUID(), Instant.now(), projectKey);
    }
}
