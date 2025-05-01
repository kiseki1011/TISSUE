package com.tissue.api.comment.presentation.dto.response;

import com.tissue.api.comment.domain.ReviewComment;
import com.tissue.api.issue.domain.Issue;

import lombok.Builder;

@Builder
public record ReviewCommentResponse(
	String workspaceCode,
	String issueKey,
	Long reviewId,
	Long commentId
) {
	public static ReviewCommentResponse from(Issue issue, ReviewComment comment) {
		return ReviewCommentResponse.builder()
			.workspaceCode(issue.getWorkspaceCode())
			.issueKey(issue.getIssueKey())
			.reviewId(comment.getReview().getId())
			.commentId(comment.getId())
			.build();
	}
}
