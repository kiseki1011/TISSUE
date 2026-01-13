package com.tissue.comment.domain.event;

import com.tissue.common.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record IssueCommentAddedEvent(
        UUID eventId,
        Instant occurredAt,
        String workspaceKey,
        String projectKey,
        String issueKey,
        Long issueId,
        Long commentId,
        Long actorMemberId,
        String actorDisplayName)
        implements DomainEvent {

    public static IssueCommentAddedEvent create(
            String workspaceKey,
            String projectKey,
            String issueKey,
            Long issueId,
            Long commentId,
            Long actorMemberId,
            String actorDisplayName) {
        return new IssueCommentAddedEvent(
                UUID.randomUUID(),
                Instant.now(),
                workspaceKey,
                projectKey,
                issueKey,
                issueId,
                commentId,
                actorMemberId,
                actorDisplayName);
    }
}
