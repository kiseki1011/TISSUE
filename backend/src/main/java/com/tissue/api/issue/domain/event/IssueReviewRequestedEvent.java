package com.tissue.api.issue.domain.event;

import com.tissue.api.issue.domain.model.Issue;
import com.tissue.api.issue.domain.model.enums.IssueType;
import com.tissue.api.notification.domain.enums.NotificationType;
import com.tissue.api.notification.domain.enums.ResourceType;

import lombok.Getter;

@Getter
public class IssueReviewRequestedEvent extends IssueEvent {

	public IssueReviewRequestedEvent(
		Long issueId,
		String issueKey,
		String workspaceCode,
		IssueType issueType,
		Long actorMemberId
	) {
		super(
			NotificationType.ISSUE_REVIEW_REQUESTED,
			ResourceType.ISSUE,
			workspaceCode,
			issueId,
			issueKey,
			issueType,
			actorMemberId
		);
	}

	public static IssueReviewRequestedEvent createEvent(
		Issue issue,
		Long actorMemberId
	) {
		return new IssueReviewRequestedEvent(
			issue.getId(),
			issue.getIssueKey(),
			issue.getWorkspaceCode(),
			issue.getType(),
			actorMemberId
		);
	}
}
