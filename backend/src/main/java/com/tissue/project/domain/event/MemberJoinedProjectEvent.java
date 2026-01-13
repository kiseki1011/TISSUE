package com.tissue.project.domain.event;

import com.tissue.common.event.DomainEvent;
import com.tissue.project.domain.ProjectMember;
import java.time.Instant;
import java.util.UUID;

public record MemberJoinedProjectEvent(
        UUID eventId,
        Instant occurredAt,
        String workspaceKey,
        String projectKey,
        Long memberId,
        String memberEmail,
        String memberDisplayName,
        Long actorMemberId)
        implements DomainEvent {

    public static MemberJoinedProjectEvent create(
            String workspaceKey,
            String projectKey,
            Long memberId,
            String memberEmail,
            String memberDisplayName,
            Long actorMemberId) {
        return new MemberJoinedProjectEvent(
                UUID.randomUUID(),
                Instant.now(),
                workspaceKey,
                projectKey,
                memberId,
                memberEmail,
                memberDisplayName,
                actorMemberId);
    }
}
