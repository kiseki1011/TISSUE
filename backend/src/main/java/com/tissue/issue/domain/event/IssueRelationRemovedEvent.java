package com.tissue.issue.domain.event;

import com.tissue.issue.domain.enums.IssueRelationType;
import java.time.Instant;
import java.util.UUID;

public record IssueRelationRemovedEvent(
        UUID eventId,
        Instant occurredAt,
        String workspaceKey,
        String sourceProjectKey,
        String sourceIssueKey,
        Long sourceIssueId,
        String targetProjectKey,
        String targetIssueKey,
        Long targetIssueId,
        Long relationId,
        IssueRelationType relationType,
        Long actorMemberId,
        String actorDisplayName) {

    public static IssueRelationRemovedEvent create(
            String workspaceKey,
            String sourceProjectKey,
            String sourceIssueKey,
            Long sourceIssueId,
            String targetProjectKey,
            String targetIssueKey,
            Long targetIssueId,
            Long relationId,
            IssueRelationType relationType,
            Long actorMemberId,
            String actorDisplayName) {
        return new IssueRelationRemovedEvent(
                UUID.randomUUID(),
                Instant.now(),
                workspaceKey,
                sourceProjectKey,
                sourceIssueKey,
                sourceIssueId,
                targetProjectKey,
                targetIssueKey,
                targetIssueId,
                relationId,
                relationType,
                actorMemberId,
                actorDisplayName);
    }
}
