package com.tissue.api.review.presentation.dto.request;

/**
 * A request to add reviewers for an Issue
 * @param reviewerIds - The id must be a WORKSPACE_MEMBER_ID
 */
public record AddReviewersRequest(
	@NotNull(message = "At least one reviewer is required")
	Long reviewerId
) {
}
