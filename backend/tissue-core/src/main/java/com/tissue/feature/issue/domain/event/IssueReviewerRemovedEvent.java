package com.tissue.feature.issue.domain.event;

import java.time.Instant;
import java.util.UUID;

public record IssueReviewerRemovedEvent(
        UUID eventId,
        Instant occurredAt,
        String projectKey,
        String issueKey,
        Long removedReviewerMemberId,
        String removedReviewerDisplayName,
        Long actorMemberId,
        String actorDisplayName) {

    public static IssueReviewerRemovedEvent create(
            String projectKey,
            String issueKey,
            Long removedReviewerMemberId,
            String removedReviewerDisplayName,
            Long actorMemberId,
            String actorDisplayName) {
        return new IssueReviewerRemovedEvent(
                UUID.randomUUID(),
                Instant.now(),
                projectKey,
                issueKey,
                removedReviewerMemberId,
                removedReviewerDisplayName,
                actorMemberId,
                actorDisplayName);
    }
}
