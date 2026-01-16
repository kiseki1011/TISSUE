package com.tissue.project.domain.event;

import com.tissue.common.event.DomainEvent;
import com.tissue.project.domain.enums.ProjectRole;
import java.time.Instant;
import java.util.UUID;

public record ProjectRoleChangedEvent(
        UUID eventId,
        Instant occurredAt,
        String workspaceKey,
        String projectKey,
        Long targetMemberId,
        ProjectRole oldRole,
        ProjectRole newRole,
        Long actorMemberId,
        String actorDisplayName,
        String targetDisplayName)
        implements DomainEvent {

    public static ProjectRoleChangedEvent create(
            String workspaceKey,
            String projectKey,
            Long targetMemberId,
            ProjectRole oldRole,
            ProjectRole newRole,
            Long actorMemberId,
            String actorDisplayName,
            String targetDisplayName) {
        return new ProjectRoleChangedEvent(
                UUID.randomUUID(),
                Instant.now(),
                workspaceKey,
                projectKey,
                targetMemberId,
                oldRole,
                newRole,
                actorMemberId,
                actorDisplayName,
                targetDisplayName);
    }
}
