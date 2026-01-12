package com.tissue.issue.domain.event;

import com.tissue.common.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record IssueCreatedEvent(
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

    public static IssueCreatedEvent create(
            String workspaceKey,
            String projectKey,
            String issueKey,
            Long issueId,
            @Nullable String parentKey,
            @Nullable Long parentId,
            Long actorMemberId,
            String actorDisplayName) {
        return new IssueCreatedEvent(
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
