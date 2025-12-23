package com.tissue.comment.presentation.dto.request;

import com.tissue.common.validator.annotation.size.LongText;

import jakarta.validation.constraints.NotBlank;

public record UpdateReviewCommentRequest(
	@NotBlank @LongText
	String content
) {
}
