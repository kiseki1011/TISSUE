package com.tissue.api.issue.domain.event;

import com.tissue.api.issue.domain.model.Issue;
import com.tissue.api.notification.domain.enums.NotificationType;
import com.tissue.api.notification.domain.enums.ResourceType;

import lombok.Getter;

@Getter
public class IssueAssignedEvent extends IssueEvent {

	private final Long assignedMemberId;

	public IssueAssignedEvent(
		Long issueId,
		String issueKey,
		String workspaceCode,
		// IssueType issueType,
		Long actorMemberId,
		Long assignedMemberId
	) {
		super(
			NotificationType.ISSUE_ASSIGNED,
			ResourceType.ISSUE,
			workspaceCode,
			issueId,
			issueKey,
			// issueType,
			actorMemberId
		);
		this.assignedMemberId = assignedMemberId;
	}

	public static IssueAssignedEvent createEvent(
		Issue issue,
		Long assignedMemberId,
		Long actorMemberId
	) {
		return new IssueAssignedEvent(
			issue.getId(),
			issue.getIssueKey(),
			issue.getWorkspaceCode(),
			// issue.getType(),
			actorMemberId,
			assignedMemberId
		);
	}
}
