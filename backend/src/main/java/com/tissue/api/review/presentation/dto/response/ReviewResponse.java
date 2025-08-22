package com.tissue.api.review.presentation.dto.response;

import lombok.Builder;

@Builder
public record ReviewResponse(
	String workspaceCode,
	String issueKey,
	Long reviewId
) {
	// public static ReviewResponse from(Review review) {
	// 	return ReviewResponse.builder()
	// 		.workspaceKey(review.getWorkspaceCode())
	// 		.issueKey(review.getIssueKey())
	// 		.reviewId(review.getId())
	// 		.build();
	// }
}
