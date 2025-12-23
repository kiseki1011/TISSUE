package com.tissue.comment.presentation.dto.request;

import com.tissue.common.validator.annotation.size.LongText;

import jakarta.validation.constraints.NotBlank;

public record CreateReviewCommentRequest(
	@NotBlank @LongText
	String content,

	Long parentCommentId
) {
	public boolean hasParentComment() {
		return parentCommentId != null;
	}
}
