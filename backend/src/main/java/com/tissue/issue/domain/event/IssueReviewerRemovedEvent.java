package com.tissue.issue.domain.event;

import java.time.Instant;
import java.util.UUID;

public record IssueReviewerRemovedEvent(
        UUID eventId,
        Instant occurredAt,
        String workspaceKey,
        String projectKey,
        String issueKey,
        Long issueId,
        Long removedReviewerMemberId,
        String removedReviewerDisplayName,
        Long actorMemberId,
        String actorDisplayName) {

    public static IssueReviewerRemovedEvent create(
            String workspaceKey,
            String projectKey,
            String issueKey,
            Long issueId,
            Long removedReviewerMemberId,
            String removedReviewerDisplayName,
            Long actorMemberId,
            String actorDisplayName) {

        return new IssueReviewerRemovedEvent(
                UUID.randomUUID(),
                Instant.now(),
                workspaceKey,
                projectKey,
                issueKey,
                issueId,
                removedReviewerMemberId,
                removedReviewerDisplayName,
                actorMemberId,
                actorDisplayName);
    }
}
