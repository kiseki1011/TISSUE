package com.tissue.api.issue.base.presentation.dto.request;

import com.tissue.api.common.validator.annotation.size.text.ShortText;

import jakarta.validation.constraints.NotBlank;

public record AddParentIssueRequest(
	@ShortText
	@NotBlank(message = "{valid.notblank}")
	String parentIssueKey
) {
}
