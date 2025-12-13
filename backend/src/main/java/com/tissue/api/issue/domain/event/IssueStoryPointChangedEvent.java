package com.tissue.api.issue.domain.event;

import java.time.Instant;
import java.util.UUID;

import org.springframework.lang.Nullable;

import com.tissue.api.common.event.DomainEvent;
import com.tissue.api.issue.domain.Issue;

public record IssueStoryPointChangedEvent(
	UUID eventId,
	Instant occurredAt,
	String workspaceKey,
	String projectKey,
	String issueKey,
	Long issueId,
	String issueType,
	String parentKey,
	Long parentId,
	Integer oldStoryPoint,
	Integer newStoryPoint,
	Long actorMemberId
) implements DomainEvent {

	public static IssueStoryPointChangedEvent create(
		Issue issue,
		@Nullable Issue parentIssue,
		@Nullable Integer oldStoryPoint,
		Long actorMemberId
	) {
		return new IssueStoryPointChangedEvent(
			UUID.randomUUID(),
			Instant.now(),
			issue.getWorkspaceKey(),
			issue.getProjectKey(),
			issue.getKey(),
			issue.getId(),
			issue.getIssueType().getDisplayLabel(),
			parentIssue != null ? parentIssue.getKey() : null,
			parentIssue != null ? parentIssue.getId() : null,
			oldStoryPoint,
			issue.getStoryPoint(),
			actorMemberId
		);
	}
}
