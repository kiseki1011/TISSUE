package com.tissue.api.review.domain.event;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.IssueType;
import com.tissue.api.issue.domain.event.IssueEvent;
import com.tissue.api.notification.domain.enums.NotificationEntityType;
import com.tissue.api.notification.domain.enums.NotificationType;

import lombok.Getter;

@Getter
public class ReviewRequestedEvent extends IssueEvent {

	public ReviewRequestedEvent(
		Long issueId,
		String issueKey,
		String workspaceCode,
		IssueType issueType,
		Long triggeredByWorkspaceMemberId
	) {
		super(
			NotificationType.ISSUE_REVIEW_REQUESTED,
			NotificationEntityType.ISSUE,
			workspaceCode,
			issueId,
			issueKey,
			issueType,
			triggeredByWorkspaceMemberId
		);
	}

	public static ReviewRequestedEvent createEvent(
		Issue issue,
		Long triggeredByWorkspaceMemberId
	) {
		return new ReviewRequestedEvent(
			issue.getId(),
			issue.getIssueKey(),
			issue.getWorkspaceCode(),
			issue.getType(),
			triggeredByWorkspaceMemberId
		);
	}
}
