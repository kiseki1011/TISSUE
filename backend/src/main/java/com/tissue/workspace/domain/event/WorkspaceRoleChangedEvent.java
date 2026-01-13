package com.tissue.workspace.domain.event;

import com.tissue.common.event.DomainEvent;
import com.tissue.workspace.domain.enums.WorkspaceRole;
import java.time.Instant;
import java.util.UUID;

public record WorkspaceRoleChangedEvent(
        UUID eventId,
        Instant occurredAt,
        String workspaceKey,
        Long targetMemberId,
        WorkspaceRole oldRole,
        WorkspaceRole newRole,
        Long actorMemberId)
        implements DomainEvent {

    public static WorkspaceRoleChangedEvent create(
            String workspaceKey,
            Long targetMemberId,
            WorkspaceRole oldRole,
            WorkspaceRole newRole,
            Long actorMemberId) {
        return new WorkspaceRoleChangedEvent(
                UUID.randomUUID(), Instant.now(), workspaceKey, targetMemberId, oldRole, newRole, actorMemberId);
    }
}
