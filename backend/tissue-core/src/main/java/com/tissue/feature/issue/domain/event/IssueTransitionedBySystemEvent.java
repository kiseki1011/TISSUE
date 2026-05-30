package com.tissue.feature.issue.domain.event;

import com.tissue.feature.vcs.domain.enums.VcsProvider;
import com.tissue.shared.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record IssueTransitionedBySystemEvent(
        UUID eventId,
        Instant occurredAt,
        String projectKey,
        String issueKey,
        @Nullable String parentKey,
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
            String projectKey,
            String issueKey,
            @Nullable String parentKey,
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
                projectKey,
                issueKey,
                parentKey,
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
