package com.tissue.feature.issue.domain.event;

import com.tissue.shared.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record IssueCreatedEvent(
        UUID eventId,
        Instant occurredAt,
        String projectKey,
        String issueKey,
        @Nullable String parentKey,
        Long actorMemberId,
        String actorDisplayName)
        implements DomainEvent {

    public static IssueCreatedEvent create(
            String projectKey,
            String issueKey,
            @Nullable String parentKey,
            Long actorMemberId,
            String actorDisplayName) {
        return new IssueCreatedEvent(
                UUID.randomUUID(), Instant.now(), projectKey, issueKey, parentKey, actorMemberId, actorDisplayName);
    }
}
