package com.tissue.api.review.presentation.dto.request;

import com.tissue.api.review.domain.enums.ReviewStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateReviewStatusRequest(
	@NotNull
	ReviewStatus status
) {
}
