package com.tissue.project.domain.event;

import com.tissue.common.event.DomainEvent;
import com.tissue.project.domain.enums.ProjectRole;
import java.time.Instant;
import java.util.UUID;

public record ProjectRoleChangedEvent(
        UUID eventId,
        Instant occurredAt,
        String workspaceKey,
        Long workspaceId,
        String projectKey,
        Long projectId,
        Long targetProjectMemberId,
        Long targetMemberId,
        String targetDisplayName,
        ProjectRole oldRole,
        ProjectRole newRole,
        Long actorMemberId,
        String actorDisplayName)
        implements DomainEvent {

    public static ProjectRoleChangedEvent create(
            String workspaceKey,
            Long workspaceId,
            String projectKey,
            Long projectId,
            Long targetProjectMemberId,
            Long targetMemberId,
            String targetDisplayName,
            ProjectRole oldRole,
            ProjectRole newRole,
            Long actorMemberId,
            String actorDisplayName) {

        return new ProjectRoleChangedEvent(
                UUID.randomUUID(),
                Instant.now(),
                workspaceKey,
                workspaceId,
                projectKey,
                projectId,
                targetProjectMemberId,
                targetMemberId,
                targetDisplayName,
                oldRole,
                newRole,
                actorMemberId,
                actorDisplayName);
    }
}
