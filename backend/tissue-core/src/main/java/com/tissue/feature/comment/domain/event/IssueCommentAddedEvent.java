package com.tissue.feature.comment.domain.event;

import com.tissue.shared.event.DomainEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record IssueCommentAddedEvent(
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

    public static IssueCommentAddedEvent create(
            String workspaceKey,
            String projectKey,
            String issueKey,
            Long commentId,
            String content,
            List<String> mentionedUsernames,
            Long actorMemberId,
            String actorDisplayName) {
        return new IssueCommentAddedEvent(
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
