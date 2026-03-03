package com.tissue.feature.comment.domain.event;

import com.tissue.shared.event.DomainEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record IssueCommentUpdatedEvent(
        UUID eventId,
        Instant occurredAt,
        String workspaceKey,
        String projectKey,
        String issueKey,
        Long commentId,
        String content,
        List<String> mentionedUsernames,
        Long actorMemberId,
        String actorDisplayName)
        implements DomainEvent {

    public static IssueCommentUpdatedEvent create(
            String workspaceKey,
            String projectKey,
            String issueKey,
            Long commentId,
            String content,
            List<String> mentionedUsernames,
            Long actorMemberId,
            String actorDisplayName) {

        return new IssueCommentUpdatedEvent(
                UUID.randomUUID(),
                Instant.now(),
                workspaceKey,
                projectKey,
                issueKey,
                commentId,
                content,
                mentionedUsernames != null ? mentionedUsernames : List.of(),
                actorMemberId,
                actorDisplayName);
    }
}
