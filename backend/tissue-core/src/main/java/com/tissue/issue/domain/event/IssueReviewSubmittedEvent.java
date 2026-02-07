package com.tissue.issue.domain.event;

import com.tissue.common.event.DomainEvent;
import com.tissue.issue.domain.enums.ReviewStatus;
import java.time.Instant;
import java.util.UUID;

public record IssueReviewSubmittedEvent(
        UUID eventId,
        Instant occurredAt,
        String workspaceKey,
        String projectKey,
        String issueKey,
        ReviewStatus reviewStatus,
        Long actorMemberId,
        String actorDisplayName)
        implements DomainEvent {

    public static IssueReviewSubmittedEvent create(
            String workspaceKey,
            String projectKey,
            String issueKey,
            ReviewStatus reviewStatus,
            Long actorMemberId,
            String actorDisplayName) {

        return new IssueReviewSubmittedEvent(
                UUID.randomUUID(),
                Instant.now(),
                workspaceKey,
                projectKey,
                issueKey,
                reviewStatus,
                actorMemberId,
                actorDisplayName);
    }
}
