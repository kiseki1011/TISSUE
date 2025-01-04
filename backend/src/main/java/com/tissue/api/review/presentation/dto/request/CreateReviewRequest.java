package com.tissue.api.review.presentation.dto.request;

import com.tissue.api.review.domain.enums.ReviewStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateReviewRequest(
	@NotNull(message = "Review status is required.")
	ReviewStatus status,

	@NotBlank(message = "Review content is required.")
	String content
) {
}
