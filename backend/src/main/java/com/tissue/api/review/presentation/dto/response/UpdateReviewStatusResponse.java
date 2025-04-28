package com.tissue.api.review.presentation.dto.response;

import java.time.LocalDateTime;

import com.tissue.api.review.domain.Review;
import com.tissue.api.review.domain.enums.ReviewStatus;

import lombok.Builder;

@Builder
public record UpdateReviewStatusResponse(
	Long reviewId,
	ReviewStatus status,
	Long updatedBy,
	LocalDateTime updatedAt
) {
	public static UpdateReviewStatusResponse from(Review review) {
		return UpdateReviewStatusResponse.builder()
			.reviewId(review.getId())
			.status(review.getStatus())
			.updatedBy(review.getLastModifiedBy())
			.updatedAt(review.getLastModifiedDate())
			.build();
	}
}
