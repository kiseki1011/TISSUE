package com.tissue.api.issue.domain.event;

import java.time.Instant;
import java.util.UUID;

import com.tissue.api.common.event.DomainEvent;
import com.tissue.api.issue.domain.Issue;

public record IssueDeletedEvent(
	UUID eventId,
	Instant occurredAt,
	String workspaceKey,
	String projectKey,
	String issueKey,
	Long issueId,
	String issueType,
	String parentKey,
	Long parentId,
	String parentIssueType,
	Long actorMemberId
) implements DomainEvent {

	public static IssueDeletedEvent create(
		Issue issue,
		Long actorMemberId
	) {
		Issue parentIssue = issue.getParentIssue();

		return new IssueDeletedEvent(
			UUID.randomUUID(),
			Instant.now(),
			issue.getWorkspaceKey(),
			issue.getProjectKey(),
			issue.getKey(),
			issue.getId(),
			issue.getIssueType().getDisplayLabel(),
			parentIssue.getKey(),
			parentIssue.getId(),
			parentIssue.getIssueType().getDisplayLabel(),
			actorMemberId
		);
	}
}
