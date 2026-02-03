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
        String targetProjectKey,
        String targetIssueKey,
        Long relationId,
        IssueRelationType relationType,
        Long actorMemberId,
        String actorDisplayName) {

    public static IssueRelationRemovedEvent create(
            String workspaceKey,
            String sourceProjectKey,
            String sourceIssueKey,
            String targetProjectKey,
            String targetIssueKey,
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
            targetProjectKey,
                targetIssueKey,
            relationId,
                relationType,
                actorMemberId,
                actorDisplayName);
    }
}
