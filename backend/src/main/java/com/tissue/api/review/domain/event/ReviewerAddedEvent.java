package com.tissue.api.review.domain.event;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.IssueType;
import com.tissue.api.issue.domain.event.IssueEvent;
import com.tissue.api.notification.domain.enums.NotificationType;
import com.tissue.api.notification.domain.enums.ResourceType;
import com.tissue.api.workspacemember.domain.WorkspaceMember;

import lombok.Getter;

@Getter
public class ReviewerAddedEvent extends IssueEvent {

	private final Long reviewerWorkspaceMemberId;
	private final String reviewerNickname;

	public ReviewerAddedEvent(
		Long issueId,
		String issueKey,
		String workspaceCode,
		IssueType issueType,
		Long triggeredByWorkspaceMemberId,
		Long reviewerWorkspaceMemberId,
		String reviewerNickname
	) {
		super(
			NotificationType.ISSUE_REVIEWER_ADDED,
			ResourceType.ISSUE,
			workspaceCode,
			issueId,
			issueKey,
			issueType,
			triggeredByWorkspaceMemberId
		);

		this.reviewerWorkspaceMemberId = reviewerWorkspaceMemberId;
		this.reviewerNickname = reviewerNickname;
	}

	// public static ReviewerAddedEvent createEvent(
	// 	Issue issue,
	// 	Long triggeredByWorkspaceMemberId,
	// 	Long reviewerWorkspaceMemberId
	// ) {
	// 	return new ReviewerAddedEvent(
	// 		issue.getId(),
	// 		issue.getIssueKey(),
	// 		issue.getWorkspaceCode(),
	// 		issue.getType(),
	// 		triggeredByWorkspaceMemberId,
	// 		reviewerWorkspaceMemberId
	// 	);
	// }

	public static ReviewerAddedEvent createEvent(
		Issue issue,
		WorkspaceMember triggeredByWorkspaceMember,
		WorkspaceMember reviewerWorkspaceMember
	) {
		return new ReviewerAddedEvent(
			issue.getId(),
			issue.getIssueKey(),
			issue.getWorkspaceCode(),
			issue.getType(),
			triggeredByWorkspaceMember.getId(),
			reviewerWorkspaceMember.getId(),
			reviewerWorkspaceMember.getNickname()
		);
	}
}
