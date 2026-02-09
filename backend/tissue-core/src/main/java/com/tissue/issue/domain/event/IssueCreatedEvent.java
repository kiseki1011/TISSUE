package com.tissue.issue.domain.event;

import com.tissue.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record IssueCreatedEvent(
        UUID eventId,
        Instant occurredAt,
        String workspaceKey,
        String projectKey,
        String issueKey,
        @Nullable String parentKey,
        Long actorMemberId,
        String actorDisplayName)
        implements DomainEvent {

    public static IssueCreatedEvent create(
            String workspaceKey,
            String projectKey,
            String issueKey,
            @Nullable String parentKey,
            Long actorMemberId,
            String actorDisplayName) {

        return new IssueCreatedEvent(
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
