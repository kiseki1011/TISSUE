package com.tissue.feature.issue.domain.event;

import java.time.Instant;
import java.util.UUID;

public record IssueReviewerAddedEvent(
        UUID eventId,
        Instant occurredAt,
        String workspaceKey,
        String projectKey,
        String issueKey,
        Long reviewerMemberId,
        String reviewerDisplayName,
        Long actorMemberId,
        String actorDisplayName) {

    public static IssueReviewerAddedEvent create(
            String workspaceKey,
            String projectKey,
            String issueKey,
            Long reviewerMemberId,
            String reviewerDisplayName,
            Long actorMemberId,
            String actorDisplayName) {
        return new IssueReviewerAddedEvent(
                UUID.randomUUID(),
                Instant.now(),
                workspaceKey,
                projectKey,
                issueKey,
                reviewerMemberId,
                reviewerDisplayName,
                actorMemberId,
                actorDisplayName);
    }
}
