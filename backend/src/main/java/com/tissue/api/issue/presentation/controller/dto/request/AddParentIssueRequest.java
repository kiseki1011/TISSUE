package com.tissue.api.issue.presentation.controller.dto.request;

import com.tissue.api.common.validator.annotation.size.text.ShortText;

import jakarta.validation.constraints.NotBlank;

public record AddParentIssueRequest(
	@ShortText
	@NotBlank(message = "{valid.notblank}")
	String parentIssueKey
) {
}
