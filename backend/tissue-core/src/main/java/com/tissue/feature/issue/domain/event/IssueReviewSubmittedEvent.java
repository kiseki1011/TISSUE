package com.tissue.feature.issue.domain.event;

import com.tissue.feature.issue.domain.enums.ReviewStatus;
import com.tissue.shared.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record IssueReviewSubmittedEvent(
        UUID eventId,
        Instant occurredAt,
        String projectKey,
        String issueKey,
        ReviewStatus reviewStatus,
        Long actorMemberId,
        String actorDisplayName)
        implements DomainEvent {

    public static IssueReviewSubmittedEvent create(
            String projectKey,
            String issueKey,
            ReviewStatus reviewStatus,
            Long actorMemberId,
            String actorDisplayName) {
        return new IssueReviewSubmittedEvent(
                UUID.randomUUID(), Instant.now(), projectKey, issueKey, reviewStatus, actorMemberId, actorDisplayName);
    }
}
