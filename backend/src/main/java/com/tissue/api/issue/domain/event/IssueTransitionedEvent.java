package com.tissue.api.issue.domain.event;

import java.time.Instant;
import java.util.UUID;

import com.tissue.api.common.util.NullSafe;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.project.domain.ProjectMember;
import com.tissue.api.workflow.domain.WorkflowState;
import com.tissue.api.workflow.domain.WorkflowTransition;

public record IssueTransitionedEvent(
	UUID eventId,
	Instant occurredAt,
	String workspaceKey,
	String projectKey,
	String issueKey,
	Long issueId,

	String parentKey,
	Long parentId,

	Long transitionId,
	String transitionName,

	Long oldStatusId,
	String oldStatusName,

	Long newStatusId,
	String newStatusName,

	Long actorMemberId,
	String actorDisplayName
) {
	public static IssueTransitionedEvent create(
		Issue issue,
		WorkflowTransition transition,
		WorkflowState oldState,
		ProjectMember actor
	) {
		Issue parentIssue = issue.getParentIssue();

		return new IssueTransitionedEvent(
			UUID.randomUUID(),
			Instant.now(),
			issue.getWorkspaceKey(),
			issue.getProjectKey(),
			issue.getKey(),
			issue.getId(),
			NullSafe.get(parentIssue, Issue::getKey),
			NullSafe.get(parentIssue, Issue::getId),
			transition.getId(),
			transition.getDisplayLabel(),
			oldState.getId(),
			oldState.getDisplayLabel(),
			transition.getTargetState().getId(),
			transition.getTargetState().getDisplayLabel(),
			actor.getMemberId(),
			actor.getDisplayName()
		);
	}
}
