package com.tissue.api.comment.presentation.dto.response;

import com.tissue.api.comment.domain.ReviewComment;

import lombok.Builder;

@Builder
public record ReviewCommentResponse(
	String workspaceCode,
	String issueKey,
	Long reviewId,
	Long commentId
) {
	public static ReviewCommentResponse from(ReviewComment comment) {
		return ReviewCommentResponse.builder()
			.workspaceCode(comment.getReview().getWorkspaceCode())
			.issueKey(comment.getReview().getIssueKey())
			.reviewId(comment.getReview().getId())
			.commentId(comment.getId())
			.build();
	}
}
