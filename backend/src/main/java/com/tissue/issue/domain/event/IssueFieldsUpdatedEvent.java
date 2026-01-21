package com.tissue.issue.domain.event;

import com.tissue.common.dto.FieldChange;
import com.tissue.common.event.DomainEvent;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record IssueFieldsUpdatedEvent(
        UUID eventId,
        Instant occurredAt,
        String workspaceKey,
        String projectKey,
        String issueKey,
        Long issueId,
        Map<String, FieldChange> changes,
        Long actorMemberId,
        String actorDisplayName)
        implements DomainEvent {

    public static IssueFieldsUpdatedEvent create(
            String workspaceKey,
            String projectKey,
            String issueKey,
            Long issueId,
            Map<String, FieldChange> changes,
            Long actorMemberId,
            String actorDisplayName) {

        return new IssueFieldsUpdatedEvent(
                UUID.randomUUID(),
                Instant.now(),
                workspaceKey,
                projectKey,
                issueKey,
                issueId,
                changes,
                actorMemberId,
                actorDisplayName);
    }
}
