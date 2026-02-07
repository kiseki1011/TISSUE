package com.tissue.workspace.domain.event;

import com.tissue.common.event.DomainEvent;
import com.tissue.workspace.domain.enums.WorkspaceRole;
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
