package com.tissue.issue.domain.event;

import com.tissue.common.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record IssueBranchLinkedEvent(
        UUID eventId,
        Instant occurredAt,
        String workspaceKey,
        String projectKey,
        String issueKey,
        String branchName,
        String repoUrl,
        String pusherName,
        @Nullable Long actorMemberId,
        @Nullable String actorDisplayName)
        implements DomainEvent {

    public static IssueBranchLinkedEvent create(
            String workspaceKey,
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
                workspaceKey,
                projectKey,
                issueKey,
            branchName,
                repoUrl,
                pusherName,
                actorMemberId,
                actorDisplayName);
    }
}
