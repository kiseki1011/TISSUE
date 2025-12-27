package com.tissue.issue.domain.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.tissue.common.dto.FieldChange;
import com.tissue.common.event.DomainEvent;
import com.tissue.issue.domain.Issue;
import com.tissue.project.domain.ProjectMember;

public record IssueFieldsUpdatedEvent(
	UUID eventId,
	Instant occurredAt,
	String workspaceKey,
	String projectKey,
	String issueKey,
	Long issueId,

	Map<String, FieldChange> changes,

	Long actorMemberId,
	String actorDisplayName
) implements DomainEvent {

	public static IssueFieldsUpdatedEvent create(
		Issue issue,
		Map<String, FieldChange> changes,
		ProjectMember actor
	) {
		return new IssueFieldsUpdatedEvent(
			UUID.randomUUID(),
			Instant.now(),
			issue.getWorkspaceKey(),
			issue.getProjectKey(),
			issue.getKey(),
			issue.getId(),
			changes,
			actor.getMemberId(),
			actor.getDisplayName()
		);
	}
}
