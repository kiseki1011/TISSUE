package com.tissue.feature.comment.domain.event;

import com.tissue.shared.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record IssueCommentDeletedEvent(
        UUID eventId,
        Instant occurredAt,
        String projectKey,
        String issueKey,
        Long commentId,
        Long actorMemberId,
        String actorDisplayName)
        implements DomainEvent {

    public static IssueCommentDeletedEvent create(
            String projectKey, String issueKey, Long commentId, Long actorMemberId, String actorDisplayName) {
        return new IssueCommentDeletedEvent(
                UUID.randomUUID(), Instant.now(), projectKey, issueKey, commentId, actorMemberId, actorDisplayName);
    }
}
