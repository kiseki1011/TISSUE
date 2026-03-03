package com.tissue.feature.issue.domain.event;

import com.tissue.feature.issue.domain.enums.IssueRelationType;
import com.tissue.shared.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record IssueRelationAddedEvent(
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
        String actorDisplayName)
        implements DomainEvent {

    public static IssueRelationAddedEvent create(
            String workspaceKey,
            String sourceProjectKey,
            String sourceIssueKey,
            String targetProjectKey,
            String targetIssueKey,
            Long relationId,
            IssueRelationType relationType,
            Long actorMemberId,
            String actorDisplayName) {

        return new IssueRelationAddedEvent(
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
