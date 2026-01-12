package com.tissue.issue.domain.event;

import com.tissue.common.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record IssueDeletedEvent(
        UUID eventId,
        Instant occurredAt,
        String workspaceKey,
        String projectKey,
        String issueKey,
        Long issueId,
        @Nullable String parentKey,
        @Nullable Long parentId,
        Long actorMemberId,
        String actorDisplayName)
        implements DomainEvent {

    public static IssueDeletedEvent create(
            String workspaceKey,
            String projectKey,
            String issueKey,
            Long issueId,
            @Nullable String parentKey,
            @Nullable Long parentId,
            Long actorMemberId,
            String actorDisplayName) {
        return new IssueDeletedEvent(
                UUID.randomUUID(),
                Instant.now(),
                workspaceKey,
                projectKey,
                issueKey,
                issueId,
                parentKey,
                parentId,
                actorMemberId,
                actorDisplayName);
    }
}
