package com.tissue.api.review.presentation.dto.response;

import com.tissue.api.review.domain.Review;

import lombok.Builder;

@Builder
public record ReviewResponse(
	String workspaceCode,
	String issueKey,
	Long reviewId
) {
	public static ReviewResponse from(Review review) {
		return ReviewResponse.builder()
			.workspaceCode(review.getWorkspaceCode())
			.issueKey(review.getIssueKey())
			.reviewId(review.getId())
			.build();
	}
}
