package com.tissue.issue.domain.event;

import com.tissue.common.event.DomainEvent;
import com.tissue.common.util.NullSafe;
import com.tissue.issue.domain.Issue;
import com.tissue.project.domain.ProjectMember;
import java.time.Instant;
import java.util.UUID;
import org.springframework.lang.Nullable;

public record IssueParentChangedEvent(
        UUID eventId,
        Instant occurredAt,
        String workspaceKey,
        String projectKey,
        String issueKey,
        Long issueId,
        String oldParentKey,
        Long oldParentId,
        String newParentKey,
        Long newParentId,
        Long actorMemberId,
        String actorDisplayName)
        implements DomainEvent {

    public static IssueParentChangedEvent create(
            Issue issue, @Nullable Issue oldParent, @Nullable Issue newParent, ProjectMember actor) {
        return new IssueParentChangedEvent(
                UUID.randomUUID(),
                Instant.now(),
                issue.getWorkspaceKey(),
                issue.getProjectKey(),
                issue.getKey(),
                issue.getId(),
                NullSafe.get(oldParent, Issue::getKey),
                NullSafe.get(oldParent, Issue::getId),
                NullSafe.get(newParent, Issue::getKey),
                NullSafe.get(newParent, Issue::getId),
                actor.getMemberId(),
                actor.getDisplayName());
    }
}
