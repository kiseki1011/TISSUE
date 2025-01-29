package com.tissue.api.comment.presentation.dto.request;

import com.tissue.api.common.validator.annotation.size.text.LongText;

import jakarta.validation.constraints.NotBlank;

public record UpdateIssueCommentRequest(
	@NotBlank @LongText
	String content
) {
}
