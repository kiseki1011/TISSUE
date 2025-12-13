package com.tissue.api.issue.domain.event;

import java.time.Instant;
import java.util.UUID;

import org.springframework.lang.Nullable;

import com.tissue.api.common.event.DomainEvent;
import com.tissue.api.issue.domain.Issue;

public record IssueParentChangedEvent(
	UUID eventId,
	Instant occurredAt,
	String workspaceKey,
	String projectKey,
	String issueKey,
	Long issueId,
	String issueType,
	String oldParentKey,
	Long oldParentId,
	String oldParentType,
	String newParentKey,
	Long newParentId,
	String newParentType,
	Long actorMemberId
) implements DomainEvent {

	public static IssueParentChangedEvent create(
		Issue issue,
		@Nullable Issue oldParent,
		@Nullable Issue newParent,
		Long actorMemberId
	) {
		return new IssueParentChangedEvent(
			UUID.randomUUID(),
			Instant.now(),
			issue.getWorkspaceKey(),
			issue.getProjectKey(),
			issue.getKey(),
			issue.getId(),
			issue.getIssueType().getDisplayLabel(),
			oldParent != null ? oldParent.getKey() : null,
			oldParent != null ? oldParent.getId() : null,
			oldParent != null ? oldParent.getIssueType().getDisplayLabel() : null,
			newParent != null ? newParent.getKey() : null,
			newParent != null ? newParent.getId() : null,
			newParent != null ? newParent.getIssueType().getDisplayLabel() : null,
			actorMemberId
		);
	}
}
