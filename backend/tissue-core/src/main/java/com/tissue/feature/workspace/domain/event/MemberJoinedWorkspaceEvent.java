package com.tissue.feature.workspace.domain.event;

import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.shared.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record MemberJoinedWorkspaceEvent(
        UUID eventId,
        Instant occurredAt,
        String workspaceKey,
        Long workspaceId,
        Long joinedWorkspaceMemberId,
        Long joinedMemberId,
        @Nullable String joinedMemberEmail,
        String joinedMemberDisplayName,
        WorkspaceRole role,
        Long actorMemberId,
        @Nullable String actorDisplayName)
        implements DomainEvent {

    public static MemberJoinedWorkspaceEvent create(
            String workspaceKey,
            Long workspaceId,
            Long joinedWorkspaceMemberId,
            Long joinedMemberId,
            @Nullable String joinedMemberEmail,
            String joinedMemberDisplayName,
            WorkspaceRole role,
            Long actorMemberId,
            @Nullable String actorDisplayName) {

        return new MemberJoinedWorkspaceEvent(
                UUID.randomUUID(),
                Instant.now(),
                workspaceKey,
                workspaceId,
                joinedWorkspaceMemberId,
                joinedMemberId,
                joinedMemberEmail,
                joinedMemberDisplayName,
                role,
                actorMemberId,
                actorDisplayName);
    }
}
