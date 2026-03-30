package com.tissue.feature.issue.domain.event;

import com.tissue.shared.event.DomainEvent;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record IssueReviewRequestedEvent(
        UUID eventId,
        Instant occurredAt,
        String workspaceKey,
        String projectKey,
        String issueKey,
        Long actorMemberId,
        String actorDisplayName,
        @Nullable Set<Long> reviewerMemberIds,
        int reviewerCount)
        implements DomainEvent {

    public static IssueReviewRequestedEvent create(
            String workspaceKey,
            String projectKey,
            String issueKey,
            Long actorMemberId,
            String actorDisplayName,
            @Nullable Set<Long> reviewerMemberIds,
            int reviewerCount) {
        return new IssueReviewRequestedEvent(
                UUID.randomUUID(),
                Instant.now(),
                workspaceKey,
                projectKey,
                issueKey,
                actorMemberId,
                actorDisplayName,
                reviewerMemberIds,
                reviewerCount);
    }
}
