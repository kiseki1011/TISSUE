package com.tissue.feature.sprint.domain.event;

import com.tissue.shared.event.DomainEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SprintIssuesAddedEvent(
        UUID eventId,
        Instant occurredAt,
        String workspaceKey,
        String projectKey,
        Long sprintId,
        List<String> issueKeys,
        Long actorMemberId,
        String actorDisplayName)
        implements DomainEvent {

    public static SprintIssuesAddedEvent create(
            String workspaceKey,
            String projectKey,
            Long sprintId,
            List<String> issueKeys,
            Long actorId,
            String actorName) {
        return new SprintIssuesAddedEvent(
                UUID.randomUUID(), Instant.now(), workspaceKey, projectKey, sprintId, issueKeys, actorId, actorName);
    }
}
