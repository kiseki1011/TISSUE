package com.tissue.sprint.domain.event;

import com.tissue.common.event.DomainEvent;
import com.tissue.project.domain.ProjectMember;
import com.tissue.sprint.domain.Sprint;
import java.time.Instant;
import java.util.UUID;

public record SprintStartedEvent(
        UUID eventId,
        Instant occurredAt,
        String workspaceKey,
        String projectKey,
        Long sprintId,
        String sprintName,
        Long actorMemberId,
        String actorDisplayName)
        implements DomainEvent {

    public static SprintStartedEvent create(
            String workspaceKey,
            String projectKey,
            Long sprintId,
            String sprintName,
            Long actorMemberId,
            String actorDisplayName) {
        return new SprintStartedEvent(
                UUID.randomUUID(),
                Instant.now(),
                workspaceKey,
                projectKey,
                sprintId,
                sprintName,
                actorMemberId,
                actorDisplayName);
    }
}
