package com.tissue.workspace.domain.event;

import com.tissue.common.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record MemberJoinedWorkspaceEvent(
        UUID eventId,
        Instant occurredAt,
        String workspaceKey,
        Long memberId,
        String memberEmail,
        String memberDisplayName)
        implements DomainEvent {

    public static MemberJoinedWorkspaceEvent create(
            String workspaceKey, Long memberId, String memberEmail, String memberDisplayName) {
        return new MemberJoinedWorkspaceEvent(
                UUID.randomUUID(), Instant.now(), workspaceKey, memberId, memberEmail, memberDisplayName);
    }
}
