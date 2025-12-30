package com.tissue.issue.domain.event;

import com.tissue.issue.domain.Issue;
import com.tissue.project.domain.ProjectMember;
import java.time.Instant;
import java.util.UUID;

public record IssueUnassignedEvent(
        UUID eventId,
        Instant occurredAt,
        String workspaceKey,
        String projectKey,
        String issueKey,
        Long issueId,
        Long removedAssigneeMemberId,
        String removedAssigneeDisplayName,
        Long actorMemberId,
        String actorDisplayName) {

    public static IssueUnassignedEvent create(Issue issue, ProjectMember removedAssignee, ProjectMember actor) {
        return new IssueUnassignedEvent(
                UUID.randomUUID(),
                Instant.now(),
                issue.getWorkspaceKey(),
                issue.getProjectKey(),
                issue.getKey(),
                issue.getId(),
                removedAssignee.getMemberId(),
                removedAssignee.getDisplayName(),
                actor.getMemberId(),
                actor.getDisplayName());
    }
}
