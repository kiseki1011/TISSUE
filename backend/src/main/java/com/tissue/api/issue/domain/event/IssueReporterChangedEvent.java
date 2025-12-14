package com.tissue.api.issue.domain.event;

import java.time.Instant;
import java.util.UUID;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.project.domain.ProjectMember;

public record IssueReporterChangedEvent(
	UUID eventId,
	Instant occurredAt,
	String workspaceKey,
	String projectKey,
	String issueKey,
	Long issueId,

	Long oldReporterMemberId,
	String oldReporterDisplayName,

	Long newReporterMemberId,
	String newReporterDisplayName,

	Long actorMemberId,
	String actorDisplayName
) {
	public static IssueReporterChangedEvent create(
		Issue issue,
		ProjectMember oldReporter,
		ProjectMember newReporter,
		ProjectMember actor
	) {
		return new IssueReporterChangedEvent(
			UUID.randomUUID(),
			Instant.now(),
			issue.getWorkspaceKey(),
			issue.getProjectKey(),
			issue.getKey(),
			issue.getId(),
			oldReporter.getMemberId(),
			oldReporter.getDisplayName(),
			newReporter.getMemberId(),
			newReporter.getDisplayName(),
			actor.getMemberId(),
			actor.getDisplayName()
		);
	}
}
