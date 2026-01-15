package com.tissue.workspace.domain.event;

import com.tissue.common.enums.JoinMethod;
import com.tissue.common.event.DomainEvent;
import com.tissue.workspace.domain.enums.WorkspaceRole;
import java.time.Instant;
import java.util.UUID;

// TODO: consider adding actorDisplayName for context
public record MemberJoinedWorkspaceEvent(
        UUID eventId,
        Instant occurredAt,
        String workspaceKey,
        Long workspaceId,
        Long joinedMemberId,
        String joinedMemberEmail,
        String joinedMemberDisplayName,
        WorkspaceRole role,
        JoinMethod joinMethod,
        Long actorMemberId)
        implements DomainEvent {

    public static MemberJoinedWorkspaceEvent create(
            String workspaceKey,
            Long workspaceId,
            Long memberId,
            String memberEmail,
            String memberDisplayName,
            WorkspaceRole role,
            JoinMethod joinMethod,
            Long actorMemberId) {
        return new MemberJoinedWorkspaceEvent(
                UUID.randomUUID(),
                Instant.now(),
                workspaceKey,
                workspaceId,
                memberId,
                memberEmail,
                memberDisplayName,
                role,
                joinMethod,
                actorMemberId);
    }
}
