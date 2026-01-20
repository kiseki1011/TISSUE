package com.tissue.issue.domain.event;

import java.time.Instant;
import java.util.UUID;

public record IssueUnassignedEvent(
        UUID eventId,
        Instant occurredAt,
        String workspaceKey,
        String projectKey,
        String issueKey,
        Long issueId,
        Long removedAssigneeMemberId,
        String removedAssigneeDisplayName,
        Long actorMemberId,
        String actorDisplayName) {

    public static IssueUnassignedEvent create(
            String workspaceKey,
            String projectKey,
            String issueKey,
            Long issueId,
            Long removedAssigneeMemberId,
            String removedAssigneeDisplayName,
            Long actorMemberId,
            String actorDisplayName) {

        return new IssueUnassignedEvent(
                UUID.randomUUID(),
                Instant.now(),
                workspaceKey,
                projectKey,
                issueKey,
                issueId,
                removedAssigneeMemberId,
                removedAssigneeDisplayName,
                actorMemberId,
                actorDisplayName);
    }
}
