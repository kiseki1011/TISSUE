package com.tissue.issue.domain.event;

import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record IssueTransitionedEvent(
        UUID eventId,
        Instant occurredAt,
        String workspaceKey,
        String projectKey,
        String issueKey,
        Long issueId,
        @Nullable String parentKey,
        @Nullable Long parentId,
        Long transitionId,
        String transitionName,
        Long oldStatusId,
        String oldStatusName,
        Long newStatusId,
        String newStatusName,
        Long actorMemberId,
        String actorDisplayName) {

    public static IssueTransitionedEvent create(
            String workspaceKey,
            String projectKey,
            String issueKey,
            Long issueId,
            @Nullable String parentKey,
            @Nullable Long parentId,
            Long transitionId,
            String transitionName,
            Long oldStatusId,
            String oldStatusName,
            Long newStatusId,
            String newStatusName,
            Long actorMemberId,
            String actorDisplayName) {

        return new IssueTransitionedEvent(
                UUID.randomUUID(),
                Instant.now(),
                workspaceKey,
                projectKey,
                issueKey,
                issueId,
                parentKey,
                parentId,
                transitionId,
                transitionName,
                oldStatusId,
                oldStatusName,
                newStatusId,
                newStatusName,
                actorMemberId,
                actorDisplayName);
    }
}
