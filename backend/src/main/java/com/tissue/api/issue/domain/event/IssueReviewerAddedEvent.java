package com.tissue.api.issue.domain.event;

import com.tissue.api.issue.domain.model.Issue;
import com.tissue.api.notification.domain.enums.NotificationType;
import com.tissue.api.notification.domain.enums.ResourceType;
import com.tissue.api.workspacemember.domain.model.WorkspaceMember;

import lombok.Getter;

@Getter
public class IssueReviewerAddedEvent extends IssueEvent {

	private final Long reviewerMemberId;
	private final String reviewerNickname;

	public IssueReviewerAddedEvent(
		Long issueId,
		String issueKey,
		String workspaceCode,
		// IssueType issueType,
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
			// issueType,
			actorMemberId
		);

		this.reviewerMemberId = reviewerMemberId;
		this.reviewerNickname = reviewerNickname;
	}

	// TODO: WorkspaceMember 대신 바로 memberId를 받을 수 있도록 변경?
	public static IssueReviewerAddedEvent createEvent(
		Issue issue,
		WorkspaceMember actor,
		WorkspaceMember reviewer
	) {
		return new IssueReviewerAddedEvent(
			issue.getId(),
			issue.getKey(),
			issue.getWorkspaceKey(),
			// issue.getType(),
			actor.getMember().getId(),
			reviewer.getMember().getId(),
			reviewer.getDisplayName()
		);
	}
}
