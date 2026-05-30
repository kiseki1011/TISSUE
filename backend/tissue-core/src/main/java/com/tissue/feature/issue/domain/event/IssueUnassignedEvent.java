package com.tissue.feature.issue.domain.event;

import java.time.Instant;
import java.util.UUID;

public record IssueUnassignedEvent(
        UUID eventId,
        Instant occurredAt,
        String projectKey,
        String issueKey,
        Long removedAssigneeMemberId,
        String removedAssigneeDisplayName,
        Long actorMemberId,
        String actorDisplayName) {

    public static IssueUnassignedEvent create(
            String projectKey,
            String issueKey,
            Long removedAssigneeMemberId,
            String removedAssigneeDisplayName,
            Long actorMemberId,
            String actorDisplayName) {
        return new IssueUnassignedEvent(
                UUID.randomUUID(),
                Instant.now(),
                projectKey,
                issueKey,
                removedAssigneeMemberId,
                removedAssigneeDisplayName,
                actorMemberId,
                actorDisplayName);
    }
}
