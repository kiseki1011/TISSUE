package com.tissue.feature.comment.domain.event;

import com.tissue.shared.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record IssueCommentDeletedEvent(
        UUID eventId,
        Instant occurredAt,
        String workspaceKey,
        String projectKey,
        String issueKey,
        Long commentId,
        Long actorMemberId,
        String actorDisplayName)
        implements DomainEvent {

    public static IssueCommentDeletedEvent create(
            String workspaceKey,
            String projectKey,
            String issueKey,
            Long commentId,
            Long actorMemberId,
            String actorDisplayName) {

        return new IssueCommentDeletedEvent(
                UUID.randomUUID(),
                Instant.now(),
                workspaceKey,
                projectKey,
                issueKey,
                commentId,
                actorMemberId,
                actorDisplayName);
    }
}
