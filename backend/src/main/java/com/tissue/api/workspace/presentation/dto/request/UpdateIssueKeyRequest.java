package com.tissue.api.workspace.presentation.dto.request;

import com.tissue.api.common.validator.annotation.pattern.IssueKeyPrefixPattern;
import com.tissue.api.common.validator.annotation.size.IssueKeyPrefixSize;

import jakarta.validation.constraints.NotBlank;

public record UpdateIssueKeyRequest(
	@NotBlank(message = "{valid.notblank}")
	@IssueKeyPrefixSize
	@IssueKeyPrefixPattern
	String issueKeyPrefix
) {
}
