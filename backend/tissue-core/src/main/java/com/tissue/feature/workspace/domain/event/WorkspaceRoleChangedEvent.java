package com.tissue.feature.workspace.domain.event;

import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.shared.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record WorkspaceRoleChangedEvent(
        UUID eventId,
        Instant occurredAt,
        String workspaceKey,
        Long targetWorkspaceMemberId,
        Long targetMemberId,
        String targetDisplayName,
        WorkspaceRole oldRole,
        WorkspaceRole newRole,
        Long actorMemberId,
        String actorDisplayName)
        implements DomainEvent {

    public static WorkspaceRoleChangedEvent create(
            String workspaceKey,
            Long targetWorkspaceMemberId,
            Long targetMemberId,
            String targetDisplayName,
            WorkspaceRole oldRole,
            WorkspaceRole newRole,
            Long actorMemberId,
            String actorDisplayName) {
        return new WorkspaceRoleChangedEvent(
                UUID.randomUUID(),
                Instant.now(),
                workspaceKey,
                targetWorkspaceMemberId,
                targetMemberId,
                targetDisplayName,
                oldRole,
                newRole,
                actorMemberId,
                actorDisplayName);
    }
}
