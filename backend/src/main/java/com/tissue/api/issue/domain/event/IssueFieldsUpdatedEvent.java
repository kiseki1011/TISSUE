package com.tissue.api.issue.domain.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.tissue.api.common.dto.FieldChange;
import com.tissue.api.common.event.DomainEvent;
import com.tissue.api.issue.domain.Issue;

public record IssueFieldsUpdatedEvent(
	UUID eventId,
	Instant occurredAt,
	String workspaceKey,
	String projectKey,
	String issueKey,
	Long issueId,
	String issueType,
	Map<String, FieldChange> changes, // Key: 필드명, Value: 변경 전후 값
	Long actorMemberId
) implements DomainEvent {

	public static IssueFieldsUpdatedEvent create(
		Issue issue,
		Map<String, FieldChange> changes,
		Long actorMemberId
	) {
		return new IssueFieldsUpdatedEvent(
			UUID.randomUUID(),
			Instant.now(),
			issue.getWorkspaceKey(),
			issue.getProjectKey(),
			issue.getKey(),
			issue.getId(),
			issue.getIssueType().getDisplayLabel(),
			changes,
			actorMemberId
		);
	}
}
