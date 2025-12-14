package com.tissue.api.issue.domain.event;

import java.time.Instant;
import java.util.UUID;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.project.domain.ProjectMember;

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
	String actorDisplayName
) {

	public static IssueUnassignedEvent create(
		Issue issue,
		ProjectMember removedAssignee,
		ProjectMember actor
	) {
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
			actor.getDisplayName()
		);
	}
}
