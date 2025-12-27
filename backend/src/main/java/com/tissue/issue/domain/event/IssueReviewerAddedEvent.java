package com.tissue.issue.domain.event;

import com.tissue.issue.domain.Issue;
import com.tissue.project.domain.ProjectMember;
import java.time.Instant;
import java.util.UUID;

public record IssueReviewerAddedEvent(
        UUID eventId,
        Instant occurredAt,
        String workspaceKey,
        String projectKey,
        String issueKey,
        Long issueId,
        Long reviewerMemberId,
        String reviewerDisplayName,
        Long actorMemberId,
        String actorDisplayName) {

    public static IssueReviewerAddedEvent create(
            Issue issue, ProjectMember reviewer, ProjectMember actor) {
        return new IssueReviewerAddedEvent(
                UUID.randomUUID(),
                Instant.now(),
                issue.getWorkspaceKey(),
                issue.getProjectKey(),
                issue.getKey(),
                issue.getId(),
                reviewer.getMemberId(),
                reviewer.getDisplayName(),
                actor.getMemberId(),
                actor.getDisplayName());
    }
}
