package com.tissue.issue.domain.event;

import com.tissue.common.event.DomainEvent;
import com.tissue.vcs.domain.enums.VcsProvider;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record IssueTransitionedBySystemEvent(
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
        Long oldStateId,
        String oldStateName,
        Long newStateId,
        String newStateName,
        VcsProvider vcsProvider,
        @Nullable String vcsUserEmail,
        @Nullable String vcsUserName,
        String triggerReason)
        implements DomainEvent {

    public static IssueTransitionedBySystemEvent create(
            String workspaceKey,
            String projectKey,
            String issueKey,
            Long issueId,
            @Nullable String parentKey,
            @Nullable Long parentId,
            Long transitionId,
            String transitionName,
            Long oldStateId,
            String oldStateName,
            Long newStateId,
            String newStateName,
            VcsProvider vcsProvider,
            @Nullable String vcsUserEmail,
            @Nullable String vcsUserName,
            String triggerReason) {

        return new IssueTransitionedBySystemEvent(
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
                oldStateId,
                oldStateName,
                newStateId,
                newStateName,
                vcsProvider,
                vcsUserEmail,
                vcsUserName,
                triggerReason);
    }
}
