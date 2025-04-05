package com.tissue.api.review.domain.event;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.IssueType;
import com.tissue.api.issue.domain.event.IssueEvent;
import com.tissue.api.notification.domain.enums.NotificationEntityType;
import com.tissue.api.notification.domain.enums.NotificationType;

import lombok.Getter;

@Getter
public class ReviewSubmittedEvent extends IssueEvent {

	private final Long reviewId;

	public ReviewSubmittedEvent(
		Long issueId,
		String issueKey,
		String workspaceCode,
		IssueType issueType,
		Long triggeredByWorkspaceMemberId,
		Long reviewId
	) {
		super(
			NotificationType.REVIEW_SUBMITTED,
			NotificationEntityType.ISSUE,
			workspaceCode,
			issueId,
			issueKey,
			issueType,
			triggeredByWorkspaceMemberId
		);

		this.reviewId = reviewId;
	}

	public static ReviewSubmittedEvent createEvent(
		Issue issue,
		Long triggeredByWorkspaceMemberId,
		Long reviewId
	) {
		return new ReviewSubmittedEvent(
			issue.getId(),
			issue.getIssueKey(),
			issue.getWorkspaceCode(),
			issue.getType(),
			triggeredByWorkspaceMemberId,
			reviewId
		);
	}
}
