package com.tissue.api.review.domain.event;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.IssueType;
import com.tissue.api.issue.domain.event.IssueEvent;
import com.tissue.api.notification.domain.enums.NotificationEntityType;
import com.tissue.api.notification.domain.enums.NotificationType;

import lombok.Getter;

@Getter
public class ReviewerAddedEvent extends IssueEvent {

	private final Long reviewerWorkspaceMemberId;

	public ReviewerAddedEvent(
		Long issueId,
		String issueKey,
		String workspaceCode,
		IssueType issueType,
		Long triggeredByWorkspaceMemberId,
		Long reviewerWorkspaceMemberId
	) {
		super(
			NotificationType.ISSUE_REVIEWER_ADDED,
			NotificationEntityType.ISSUE,
			workspaceCode,
			issueId,
			issueKey,
			issueType,
			triggeredByWorkspaceMemberId
		);

		this.reviewerWorkspaceMemberId = reviewerWorkspaceMemberId;
	}

	public static ReviewerAddedEvent createEvent(
		Issue issue,
		Long triggeredByWorkspaceMemberId,
		Long reviewerWorkspaceMemberId
	) {
		return new ReviewerAddedEvent(
			issue.getId(),
			issue.getIssueKey(),
			issue.getWorkspaceCode(),
			issue.getType(),
			triggeredByWorkspaceMemberId,
			reviewerWorkspaceMemberId
		);
	}
}
