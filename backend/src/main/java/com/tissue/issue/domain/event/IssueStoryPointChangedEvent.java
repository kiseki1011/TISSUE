package com.tissue.issue.domain.event;

import com.tissue.common.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record IssueStoryPointChangedEvent(
        UUID eventId,
        Instant occurredAt,
        String workspaceKey,
        String projectKey,
        String issueKey,
        Long issueId,
        @Nullable String parentKey,
        @Nullable Long parentId,
        @Nullable Integer oldStoryPoint,
        @Nullable Integer newStoryPoint,
        Long actorMemberId,
        String actorDisplayName)
        implements DomainEvent {

    public static IssueStoryPointChangedEvent create(
            String workspaceKey,
            String projectKey,
            String issueKey,
            Long issueId,
            @Nullable String parentKey,
            @Nullable Long parentId,
            @Nullable Integer oldStoryPoint,
            @Nullable Integer newStoryPoint,
            Long actorMemberId,
            String actorDisplayName) {

        return new IssueStoryPointChangedEvent(
                UUID.randomUUID(),
                Instant.now(),
                workspaceKey,
                projectKey,
                issueKey,
                issueId,
                parentKey,
                parentId,
                oldStoryPoint,
                newStoryPoint,
                actorMemberId,
                actorDisplayName);
    }
}
