package com.tissue.issue.domain.event;

import com.tissue.common.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record IssueParentChangedEvent(
        UUID eventId,
        Instant occurredAt,
        String workspaceKey,
        String projectKey,
        String issueKey,
        Long issueId,
        @Nullable String oldParentKey,
        @Nullable Long oldParentId,
        @Nullable String newParentKey,
        @Nullable Long newParentId,
        Long actorMemberId,
        String actorDisplayName)
        implements DomainEvent {

    public static IssueParentChangedEvent create(
            String workspaceKey,
            String projectKey,
            String issueKey,
            Long issueId,
            @Nullable String oldParentKey,
            @Nullable Long oldParentId,
            @Nullable String newParentKey,
            @Nullable Long newParentId,
            Long actorMemberId,
            String actorDisplayName) {

        return new IssueParentChangedEvent(
                UUID.randomUUID(),
                Instant.now(),
                workspaceKey,
                projectKey,
                issueKey,
                issueId,
                oldParentKey,
                oldParentId,
                newParentKey,
                newParentId,
                actorMemberId,
                actorDisplayName);
    }
}
