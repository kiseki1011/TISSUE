package com.tissue.api.comment.domain.event;

import com.tissue.api.notification.domain.enums.NotificationType;
import com.tissue.api.notification.domain.enums.ResourceType;
import com.tissue.api.notification.domain.model.vo.EntityReference;

import lombok.Getter;

@Getter
public class ReviewCommentAddedEvent extends CommentEvent {

	private final Long reviewId;

	public ReviewCommentAddedEvent(
		Long issueId,
		String issueKey,
		String workspaceCode,
		// IssueType issueType,
		Long actorMemberId,
		Long reviewId,
		Long commentId
	) {
		super(
			NotificationType.REVIEW_COMMENT_ADDED,
			ResourceType.ISSUE,
			workspaceCode,
			issueId,
			issueKey,
			// issueType,
			actorMemberId,
			commentId
		);

		this.reviewId = reviewId;
	}

	// public static ReviewCommentAddedEvent createEvent(
	// 	Issue issue,
	// 	Review review,
	// 	ReviewComment comment,
	// 	Long actorMemberId
	// ) {
	// 	return new ReviewCommentAddedEvent(
	// 		issue.getId(),
	// 		issue.getIssueKey(),
	// 		issue.getWorkspaceCode(),
	// 		// issue.getType(),
	// 		actorMemberId,
	// 		review.getId(),
	// 		comment.getId()
	// 	);
	// }

	@Override
	public EntityReference createEntityReference() {
		return EntityReference.forReviewComment(
			getWorkspaceCode(),
			getIssueKey(),
			getReviewId(),
			getCommentId()
		);
	}
}
