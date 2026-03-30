package com.tissue.feature.sprint.domain.event;

import com.tissue.shared.event.DomainEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SprintIssuesRemovedEvent(
        UUID eventId,
        Instant occurredAt,
        String workspaceKey,
        String projectKey,
        Long sprintId,
        List<String> issueKeys,
        Long actorMemberId,
        String actorDisplayName)
        implements DomainEvent {

    public static SprintIssuesRemovedEvent create(
            String workspaceKey,
            String projectKey,
            Long sprintId,
            List<String> issueKeys,
            Long actorId,
            String actorName) {
        return new SprintIssuesRemovedEvent(
                UUID.randomUUID(), Instant.now(), workspaceKey, projectKey, sprintId, issueKeys, actorId, actorName);
    }
}
