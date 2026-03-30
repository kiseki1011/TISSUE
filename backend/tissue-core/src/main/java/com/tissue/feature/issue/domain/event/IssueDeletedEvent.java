package com.tissue.feature.issue.domain.event;

import com.tissue.shared.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record IssueDeletedEvent(
        UUID eventId,
        Instant occurredAt,
        String workspaceKey,
        String projectKey,
        String issueKey,
        @Nullable String parentKey,
        Long actorMemberId,
        String actorDisplayName)
        implements DomainEvent {

    public static IssueDeletedEvent create(
            String workspaceKey,
            String projectKey,
            String issueKey,
            @Nullable String parentKey,
            Long actorMemberId,
            String actorDisplayName) {
        return new IssueDeletedEvent(
                UUID.randomUUID(),
                Instant.now(),
                workspaceKey,
                projectKey,
                issueKey,
                parentKey,
                actorMemberId,
                actorDisplayName);
    }
}
