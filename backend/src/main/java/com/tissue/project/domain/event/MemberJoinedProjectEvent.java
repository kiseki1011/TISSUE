package com.tissue.project.domain.event;

import com.tissue.common.enums.JoinMethod;
import com.tissue.common.event.DomainEvent;
import com.tissue.project.domain.enums.ProjectRole;
import java.time.Instant;
import java.util.UUID;

public record MemberJoinedProjectEvent(
        UUID eventId,
        Instant occurredAt,
        String workspaceKey,
        Long workspaceId,
        String projectKey,
        Long projectId,
        Long joinedProjectMemberId,
        Long joinedMemberId,
        String joinedMemberDisplayName,
        ProjectRole role,
        JoinMethod joinMethod,
        Long actorMemberId,
        String actorDisplayName)
        implements DomainEvent {

    public static MemberJoinedProjectEvent create(
            String workspaceKey,
            Long workspaceId,
            String projectKey,
            Long projectId,
            Long joinedProjectMemberId,
            Long joinedMemberId,
            String joinedMemberDisplayName,
            ProjectRole role,
            JoinMethod joinMethod,
            Long actorMemberId,
            String actorDisplayName) {

        return new MemberJoinedProjectEvent(
                UUID.randomUUID(),
                Instant.now(),
                workspaceKey,
                workspaceId,
                projectKey,
                projectId,
                joinedProjectMemberId,
                joinedMemberId,
                joinedMemberDisplayName,
                role,
                joinMethod,
                actorMemberId,
                actorDisplayName);
    }
}
