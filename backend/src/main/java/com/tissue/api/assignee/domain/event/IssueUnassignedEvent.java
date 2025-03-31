package com.tissue.api.assignee.domain.event;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.IssueType;
import com.tissue.api.issue.domain.event.IssueEvent;
import com.tissue.api.notification.domain.enums.NotificationEntityType;
import com.tissue.api.notification.domain.enums.NotificationType;

import lombok.Getter;

@Getter
public class IssueUnassignedEvent extends IssueEvent {

	private final Long assigneeWorkspaceMemberId;

	public IssueUnassignedEvent(
		Long issueId,
		String issueKey,
		String workspaceCode,
		IssueType issueType,
		Long triggeredByWorkspaceMemberId,
		Long assigneeWorkspaceMemberId
	) {
		super(
			NotificationType.ISSUE_ASSIGNED,
			NotificationEntityType.ISSUE,
			issueId, issueKey,
			workspaceCode,
			issueType,
			triggeredByWorkspaceMemberId
		);
		this.assigneeWorkspaceMemberId = assigneeWorkspaceMemberId;
	}

	public static IssueUnassignedEvent createEvent(
		Issue issue,
		Long assigneeWorkspaceMemberId,
		Long triggeredByWorkspaceMemberId
	) {
		return new IssueUnassignedEvent(
			issue.getId(),
			issue.getIssueKey(),
			issue.getWorkspaceCode(),
			issue.getType(),
			triggeredByWorkspaceMemberId,
			assigneeWorkspaceMemberId
		);
	}
}
