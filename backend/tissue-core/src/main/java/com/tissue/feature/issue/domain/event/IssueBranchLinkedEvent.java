package com.tissue.feature.issue.domain.event;

import com.tissue.shared.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record IssueBranchLinkedEvent(
        UUID eventId,
        Instant occurredAt,
        String projectKey,
        String issueKey,
        String branchName,
        String repoUrl,
        String pusherName,
        @Nullable Long actorMemberId,
        @Nullable String actorDisplayName)
        implements DomainEvent {

    public static IssueBranchLinkedEvent create(
            String projectKey,
            String issueKey,
            String branchName,
            String repoUrl,
            String pusherName,
            @Nullable Long actorMemberId,
            @Nullable String actorDisplayName) {
        return new IssueBranchLinkedEvent(
                UUID.randomUUID(),
                Instant.now(),
                projectKey,
                issueKey,
                branchName,
                repoUrl,
                pusherName,
                actorMemberId,
                actorDisplayName);
    }
}
