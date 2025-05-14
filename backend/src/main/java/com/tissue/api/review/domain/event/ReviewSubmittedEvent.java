package com.tissue.api.review.domain.event;

import com.tissue.api.issue.domain.model.Issue;
import com.tissue.api.issue.domain.model.enums.IssueType;
import com.tissue.api.issue.domain.event.IssueEvent;
import com.tissue.api.notification.domain.enums.NotificationType;
import com.tissue.api.notification.domain.enums.ResourceType;
import com.tissue.api.notification.domain.model.vo.EntityReference;
import com.tissue.api.review.domain.model.Review;
import com.tissue.api.review.domain.model.enums.ReviewStatus;

import lombok.Getter;

@Getter
public class ReviewSubmittedEvent extends IssueEvent {

	private final Long reviewId;
	private final ReviewStatus reviewStatus;

	public ReviewSubmittedEvent(
		Long issueId,
		String issueKey,
		String workspaceCode,
		IssueType issueType,
		Long actorMemberId,
		Long reviewId,
		ReviewStatus reviewStatus
	) {
		super(
			NotificationType.ISSUE_REVIEW_SUBMITTED,
			ResourceType.ISSUE,
			workspaceCode,
			issueId,
			issueKey,
			issueType,
			actorMemberId
		);

		this.reviewId = reviewId;
		this.reviewStatus = reviewStatus;
	}

	public static ReviewSubmittedEvent createEvent(
		Issue issue,
		Long actorMemberId,
		Review review
	) {
		return new ReviewSubmittedEvent(
			issue.getId(),
			issue.getIssueKey(),
			issue.getWorkspaceCode(),
			issue.getType(),
			actorMemberId,
			review.getId(),
			review.getStatus()
		);
	}

	@Override
	public EntityReference createEntityReference() {
		return EntityReference.forReview(getWorkspaceCode(), getIssueKey(), getReviewId());
	}
}
