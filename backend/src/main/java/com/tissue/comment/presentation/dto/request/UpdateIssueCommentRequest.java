package com.tissue.comment.presentation.dto.request;

import com.tissue.common.validator.annotation.size.text.LongText;

import jakarta.validation.constraints.NotBlank;

public record UpdateIssueCommentRequest(
	@NotBlank @LongText
	String content
) {
}
