package com.tissue.api.issue.domain.event;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.IssueType;
import com.tissue.api.notification.domain.enums.NotificationType;
import com.tissue.api.notification.domain.enums.ResourceType;

import lombok.Getter;

@Getter
public class IssueUnassignedEvent extends IssueEvent {

	private final Long assigneeMemberId;

	public IssueUnassignedEvent(
		Long issueId,
		String issueKey,
		String workspaceCode,
		IssueType issueType,
		Long actorMemberId,
		Long assigneeMemberId
	) {
		super(
			NotificationType.ISSUE_UNASSIGNED,
			ResourceType.ISSUE,
			workspaceCode,
			issueId,
			issueKey,
			issueType,
			actorMemberId
		);
		this.assigneeMemberId = assigneeMemberId;
	}

	public static IssueUnassignedEvent createEvent(
		Issue issue,
		Long assigneeMemberId,
		Long actorMemberId
	) {
		return new IssueUnassignedEvent(
			issue.getId(),
			issue.getIssueKey(),
			issue.getWorkspaceCode(),
			issue.getType(),
			actorMemberId,
			assigneeMemberId
		);
	}
}
