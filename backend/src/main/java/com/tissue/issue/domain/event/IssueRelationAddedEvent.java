package com.tissue.issue.domain.event;

import com.tissue.common.event.DomainEvent;
import com.tissue.issue.domain.Issue;
import com.tissue.issue.domain.IssueRelation;
import com.tissue.issue.domain.enums.IssueRelationType;
import com.tissue.project.domain.ProjectMember;
import java.time.Instant;
import java.util.UUID;

public record IssueRelationAddedEvent(
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
        String actorDisplayName)
        implements DomainEvent {

    public static IssueRelationAddedEvent create(
            Issue source, Issue target, IssueRelation relation, ProjectMember actor) {
        return new IssueRelationAddedEvent(
                UUID.randomUUID(),
                Instant.now(),
                source.getWorkspaceKey(),
                source.getProjectKey(),
                source.getKey(),
                source.getId(),
                target.getProjectKey(),
                target.getKey(),
                target.getId(),
                relation.getId(),
                relation.getRelationType(),
                actor.getMemberId(),
                actor.getDisplayName());
    }
}
