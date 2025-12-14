package com.tissue.api.issue.domain.event;

import java.time.Instant;
import java.util.UUID;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.IssueRelation;
import com.tissue.api.issue.domain.enums.IssueRelationType;
import com.tissue.api.project.domain.ProjectMember;

public record IssueRelationRemovedEvent(
	UUID eventId,
	Instant occurredAt,
	String workspaceKey,
	String sourceProjectKey,
	String sourceIssueKey,
	Long sourceIssueId,

	String targetProjectKey,
	String targetIssueKey,
	Long targetIssueId,

	Long relationId,
	IssueRelationType relationType,

	Long actorMemberId,
	String actorDisplayName
) {

	public static IssueRelationRemovedEvent create(
		Issue source,
		Issue target,
		IssueRelation removedRelation,
		ProjectMember actor
	) {
		return new IssueRelationRemovedEvent(
			UUID.randomUUID(),
			Instant.now(),
			source.getWorkspaceKey(),
			source.getProjectKey(),
			source.getKey(),
			source.getId(),
			target.getProjectKey(),
			target.getKey(),
			target.getId(),
			removedRelation.getId(),
			removedRelation.getRelationType(),
			actor.getMemberId(),
			actor.getDisplayName()
		);
	}
}
