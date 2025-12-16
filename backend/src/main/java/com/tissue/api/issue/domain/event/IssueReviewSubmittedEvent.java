package com.tissue.api.issue.domain.event;

import java.time.Instant;
import java.util.UUID;

import com.tissue.api.common.event.DomainEvent;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.ReviewStatus;
import com.tissue.api.project.domain.ProjectMember;

public record IssueReviewSubmittedEvent(
	UUID eventId,
	Instant occurredAt,
	String workspaceKey,
	String projectKey,
	String issueKey,
	Long issueId,

	ReviewStatus status,

	Long actorMemberId,
	String actorDisplayName
) implements DomainEvent {

	public static IssueReviewSubmittedEvent create(Issue issue, ReviewStatus status, ProjectMember actor) {
		return new IssueReviewSubmittedEvent(
			UUID.randomUUID(),
			Instant.now(),
			issue.getWorkspaceKey(),
			issue.getProjectKey(),
			issue.getKey(),
			issue.getId(),
			status,
			actor.getMemberId(),
			actor.getDisplayName()
		);
	}
}
