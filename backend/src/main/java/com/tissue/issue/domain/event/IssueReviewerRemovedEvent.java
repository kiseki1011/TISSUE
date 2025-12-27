package com.tissue.issue.domain.event;

import com.tissue.issue.domain.Issue;
import com.tissue.project.domain.ProjectMember;
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
            Issue issue, ProjectMember removedReviewer, ProjectMember actor) {
        return new IssueReviewerRemovedEvent(
                UUID.randomUUID(),
                Instant.now(),
                issue.getWorkspaceKey(),
                issue.getProjectKey(),
                issue.getKey(),
                issue.getId(),
                removedReviewer.getMemberId(),
                removedReviewer.getDisplayName(),
                actor.getMemberId(),
                actor.getDisplayName());
    }
}
