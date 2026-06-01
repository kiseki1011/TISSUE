package com.tissue.feature.project.domain.event;

import com.tissue.feature.project.domain.ProjectRole;
import com.tissue.shared.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record ProjectRoleChangedEvent(
        UUID eventId,
        Instant occurredAt,
        String projectKey,
        Long targetMemberId,
        String targetDisplayName,
        ProjectRole oldRole,
        ProjectRole newRole,
        Long actorMemberId,
        String actorDisplayName)
        implements DomainEvent {

    public static ProjectRoleChangedEvent create(
            String projectKey,
            Long targetMemberId,
            String targetDisplayName,
            ProjectRole oldRole,
            ProjectRole newRole,
            Long actorMemberId,
            String actorDisplayName) {
        return new ProjectRoleChangedEvent(
                UUID.randomUUID(),
                Instant.now(),
                projectKey,
                targetMemberId,
                targetDisplayName,
                oldRole,
                newRole,
                actorMemberId,
                actorDisplayName);
    }
}
