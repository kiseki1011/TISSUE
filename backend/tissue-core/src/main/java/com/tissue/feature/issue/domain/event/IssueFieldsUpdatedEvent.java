package com.tissue.feature.issue.domain.event;

import com.tissue.shared.dto.FieldChange;
import com.tissue.shared.event.DomainEvent;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record IssueFieldsUpdatedEvent(
        UUID eventId,
        Instant occurredAt,
        String workspaceKey,
        String projectKey,
        String issueKey,
        Map<String, FieldChange> changes,
        Long actorMemberId,
        String actorDisplayName)
        implements DomainEvent {

    public static IssueFieldsUpdatedEvent create(
            String workspaceKey,
            String projectKey,
            String issueKey,
            Map<String, FieldChange> changes,
            Long actorMemberId,
            String actorDisplayName) {
        return new IssueFieldsUpdatedEvent(
                UUID.randomUUID(),
                Instant.now(),
                workspaceKey,
                projectKey,
                issueKey,
                changes,
                actorMemberId,
                actorDisplayName);
    }
}
