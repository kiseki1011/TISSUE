package com.tissue.issue.domain.event;

import java.time.Instant;
import java.util.UUID;

import com.tissue.common.event.DomainEvent;
import com.tissue.common.util.NullSafe;
import com.tissue.issue.domain.Issue;
import com.tissue.project.domain.ProjectMember;

public record IssueDeletedEvent(
	UUID eventId,
	Instant occurredAt,
	String workspaceKey,
	String projectKey,
	String issueKey,
	Long issueId,

	String parentKey,
	Long parentId,

	Long actorMemberId,
	String actorDisplayName
) implements DomainEvent {

	public static IssueDeletedEvent create(Issue issue, ProjectMember actor) {
		Issue parentIssue = issue.getParentIssue();

		return new IssueDeletedEvent(
			UUID.randomUUID(),
			Instant.now(),
			issue.getWorkspaceKey(),
			issue.getProjectKey(),
			issue.getKey(),
			issue.getId(),
			NullSafe.get(parentIssue, Issue::getKey),
			NullSafe.get(parentIssue, Issue::getId),
			actor.getMemberId(),
			actor.getDisplayName()
		);
	}
}
