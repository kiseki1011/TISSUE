package com.tissue.issue.domain.event;

import java.time.Instant;
import java.util.UUID;

import com.tissue.issue.domain.Issue;
import com.tissue.project.domain.ProjectMember;

public record IssueAssignedEvent(
	UUID eventId,
	Instant occurredAt,
	String workspaceKey,
	String projectKey,
	String issueKey,
	Long issueId,

	Long assigneeMemberId,
	String assigneeDisplayName,

	Long actorMemberId,
	String actorDisplayName
) {

	public static IssueAssignedEvent create(
		Issue issue,
		ProjectMember assignee,
		ProjectMember actor
	) {
		return new IssueAssignedEvent(
			UUID.randomUUID(),
			Instant.now(),
			issue.getWorkspaceKey(),
			issue.getProjectKey(),
			issue.getKey(),
			issue.getId(),
			assignee.getMemberId(),
			assignee.getDisplayName(),
			actor.getMemberId(),
			actor.getDisplayName()
		);
	}
}
