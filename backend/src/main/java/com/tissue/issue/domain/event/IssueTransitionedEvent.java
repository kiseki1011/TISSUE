package com.tissue.issue.domain.event;

import java.time.Instant;
import java.util.UUID;

import com.tissue.common.util.NullSafe;
import com.tissue.issue.domain.Issue;
import com.tissue.project.domain.ProjectMember;
import com.tissue.workflow.domain.WorkflowState;
import com.tissue.workflow.domain.WorkflowTransition;

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
			transition.getDisplayName(),
			oldState.getId(),
			oldState.getDisplayName(),
			transition.getTargetState().getId(),
			transition.getTargetState().getDisplayName(),
			actor.getMemberId(),
			actor.getDisplayName()
		);
	}
}
