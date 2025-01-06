package com.tissue.api.review.presentation.dto.request;

public record UpdateReviewRequest(
	String title,
	String content
) {
}
