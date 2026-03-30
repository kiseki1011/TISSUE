package com.tissue.feature.workspace.domain.event;

import com.tissue.shared.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record WorkspaceOwnershipTransferredEvent(
        UUID eventId,
        Instant occurredAt,
        String workspaceKey,
        Long newOwnerMemberId,
        String newOwnerDisplayName,
        Long previousOwnerMemberId,
        String previousOwnerDisplayName)
        implements DomainEvent {

    public static WorkspaceOwnershipTransferredEvent create(
            String workspaceKey,
            Long newOwnerMemberId,
            String newOwnerDisplayName,
            Long previousOwnerMemberId,
            String previousOwnerDisplayName) {
        return new WorkspaceOwnershipTransferredEvent(
                UUID.randomUUID(),
                Instant.now(),
                workspaceKey,
                newOwnerMemberId,
                newOwnerDisplayName,
                previousOwnerMemberId,
                previousOwnerDisplayName);
    }
}
