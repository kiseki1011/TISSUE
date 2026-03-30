package com.tissue.feature.workspace.domain.event;

import com.tissue.shared.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record WorkspaceDeletedEvent(
        UUID eventId,
        Instant occurredAt,
        String workspaceKey,
        Long workspaceId,
        String workspaceName,
        Long actorMemberId,
        String actorDisplayName)
        implements DomainEvent {

    public static WorkspaceDeletedEvent create(
            String workspaceKey, Long workspaceId, String workspaceName, Long actorMemberId, String actorDisplayName) {
        return new WorkspaceDeletedEvent(
                UUID.randomUUID(),
                Instant.now(),
                workspaceKey,
                workspaceId,
                workspaceName,
                actorMemberId,
                actorDisplayName);
    }
}
