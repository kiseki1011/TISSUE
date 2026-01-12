package com.tissue.issue.domain.event;

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
        return new IssueReporterChangedEvent(
                UUID.randomUUID(),
                Instant.now(),
                workspaceKey,
                projectKey,
                issueKey,
                issueId,
                oldReporterMemberId,
                oldReporterDisplayName,
                newReporterMemberId,
                newReporterDisplayName,
                actorMemberId,
                actorDisplayName);
    }
}
