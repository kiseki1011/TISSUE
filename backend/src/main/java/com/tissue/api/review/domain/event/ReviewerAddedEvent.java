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

	private final Long reviewerMemberId;
	private final String reviewerNickname;

	public ReviewerAddedEvent(
		Long issueId,
		String issueKey,
		String workspaceCode,
		IssueType issueType,
		Long actorMemberId,
		Long reviewerMemberId,
		String reviewerNickname
	) {
		super(
			NotificationType.ISSUE_REVIEWER_ADDED,
			ResourceType.ISSUE,
			workspaceCode,
			issueId,
			issueKey,
			issueType,
			actorMemberId
		);

		this.reviewerMemberId = reviewerMemberId;
		this.reviewerNickname = reviewerNickname;
	}

	// TODO: WorkspaceMember 대신 바로 memberId를 받을 수 있도록 변경?
	public static ReviewerAddedEvent createEvent(
		Issue issue,
		WorkspaceMember actor,
		WorkspaceMember reviewer
	) {
		return new ReviewerAddedEvent(
			issue.getId(),
			issue.getIssueKey(),
			issue.getWorkspaceCode(),
			issue.getType(),
			actor.getMember().getId(),
			reviewer.getMember().getId(),
			reviewer.getDisplayName()
		);
	}
}
