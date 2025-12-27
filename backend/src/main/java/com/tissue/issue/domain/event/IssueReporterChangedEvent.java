package com.tissue.issue.domain.event;

import com.tissue.issue.domain.Issue;
import com.tissue.project.domain.ProjectMember;
import java.time.Instant;
import java.util.UUID;

public record IssueReporterChangedEvent(
        UUID eventId,
        Instant occurredAt,
        String workspaceKey,
        String projectKey,
        String issueKey,
        Long issueId,
        Long oldReporterMemberId,
        String oldReporterDisplayName,
        Long newReporterMemberId,
        String newReporterDisplayName,
        Long actorMemberId,
        String actorDisplayName) {
    public static IssueReporterChangedEvent create(
            Issue issue,
            ProjectMember oldReporter,
            ProjectMember newReporter,
            ProjectMember actor) {
        return new IssueReporterChangedEvent(
                UUID.randomUUID(),
                Instant.now(),
                issue.getWorkspaceKey(),
                issue.getProjectKey(),
                issue.getKey(),
                issue.getId(),
                oldReporter.getMemberId(),
                oldReporter.getDisplayName(),
                newReporter.getMemberId(),
                newReporter.getDisplayName(),
                actor.getMemberId(),
                actor.getDisplayName());
    }
}
