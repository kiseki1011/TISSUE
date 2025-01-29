package com.tissue.api.comment.presentation.dto.request;

import com.tissue.api.common.validator.annotation.size.text.LongText;

import jakarta.validation.constraints.NotBlank;

public record CreateIssueCommentRequest(
	@NotBlank @LongText
	String content,

	Long parentCommentId
) {
	public boolean hasParentComment() {
		return parentCommentId != null;
	}
}
