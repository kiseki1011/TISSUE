package com.tissue.feature.issue.domain.event;

import com.tissue.feature.vcs.domain.enums.PrAction;
import com.tissue.shared.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record IssueVcsConnectionEvent(
        UUID eventId,
        Instant occurredAt,
        String projectKey,
        String issueKey,
        String prTitle,
        String prUrl,
        PrAction prAction,
        String vcsUserEmail,
        String vcsUserName,
        Instant prOccurredAt,
        @Nullable Long actorMemberId,
        @Nullable String actorDisplayName)
        implements DomainEvent {

    public static IssueVcsConnectionEvent create(
            String projectKey,
            String issueKey,
            String prTitle,
            String prUrl,
            PrAction prAction,
            String vcsUserEmail,
            String vcsUserName,
            Instant prOccurredAt,
            @Nullable Long actorMemberId,
            @Nullable String actorDisplayName) {
        return new IssueVcsConnectionEvent(
                UUID.randomUUID(),
                Instant.now(),
                projectKey,
                issueKey,
                prTitle,
                prUrl,
                prAction,
                vcsUserEmail,
                vcsUserName,
                prOccurredAt,
                actorMemberId,
                actorDisplayName);
    }
}
