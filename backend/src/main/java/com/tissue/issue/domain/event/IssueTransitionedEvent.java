package com.tissue.issue.domain.event;

import com.tissue.common.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record IssueTransitionedEvent(
        UUID eventId,
        Instant occurredAt,
        String workspaceKey,
        String projectKey,
        String issueKey,
        @Nullable String parentKey,
        Long transitionId,
        String transitionName,
        Long oldStateId,
        String oldStateName,
        Long newStateId,
        String newStateName,
        Long actorMemberId,
        String actorDisplayName)
        implements DomainEvent {

    public static IssueTransitionedEvent create(
            String workspaceKey,
            String projectKey,
            String issueKey,
            @Nullable String parentKey,
            Long transitionId,
            String transitionName,
            Long oldStateId,
            String oldStateName,
            Long newStateId,
            String newStateName,
            Long actorMemberId,
            String actorDisplayName) {

        return new IssueTransitionedEvent(
                UUID.randomUUID(),
                Instant.now(),
                workspaceKey,
                projectKey,
                issueKey,
            parentKey,
            transitionId,
                transitionName,
                oldStateId,
                oldStateName,
                newStateId,
                newStateName,
                actorMemberId,
                actorDisplayName);
    }
}
