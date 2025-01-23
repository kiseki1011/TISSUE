package com.tissue.api.issue.presentation.dto.request;

import com.tissue.api.common.validator.annotation.size.text.ShortText;

import jakarta.validation.constraints.NotBlank;

public record AssignParentIssueRequest(
	@ShortText
	@NotBlank(message = "{valid.notblank}")
	String parentIssueKey
) {
}
