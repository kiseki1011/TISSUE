package com.tissue.api.review.presentation.dto.response;

import java.time.LocalDateTime;

import com.tissue.api.review.domain.Review;

import lombok.Builder;

@Builder
public record UpdateReviewResponse(
	Long reviewId,
	String title,
	String content,
	Long updatedBy,
	LocalDateTime updatedAt
) {
	public static UpdateReviewResponse from(Review review) {
		return UpdateReviewResponse.builder()
			.reviewId(review.getId())
			.title(review.getTitle())
			.content(review.getContent())
			.updatedBy(review.getLastModifiedByWorkspaceMember())
			.updatedAt(review.getLastModifiedDate())
			.build();
	}
}
