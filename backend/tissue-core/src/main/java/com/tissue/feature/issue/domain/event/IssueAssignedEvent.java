package com.tissue.feature.issue.domain.event;

import java.time.Instant;
import java.util.UUID;

public record IssueAssignedEvent(
        UUID eventId,
        Instant occurredAt,
        String workspaceKey,
        String projectKey,
        String issueKey,
        Long assigneeMemberId,
        String assigneeDisplayName,
        Long actorMemberId,
        String actorDisplayName) {

    public static IssueAssignedEvent create(
            String workspaceKey,
            String projectKey,
            String issueKey,
            Long assigneeMemberId,
            String assigneeDisplayName,
            Long actorMemberId,
            String actorDisplayName) {

        return new IssueAssignedEvent(
                UUID.randomUUID(),
                Instant.now(),
                workspaceKey,
                projectKey,
                issueKey,
                assigneeMemberId,
                assigneeDisplayName,
                actorMemberId,
                actorDisplayName);
    }
}
