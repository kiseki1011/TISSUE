package com.tissue.api.workspace.presentation.dto.request;

import com.tissue.api.common.validator.annotation.pattern.IssueKeyPrefixPattern;
import com.tissue.api.common.validator.annotation.size.IssueKeyPrefixSize;

import jakarta.validation.constraints.NotBlank;

public record UpdateIssueKeyRequest(

	@IssueKeyPrefixSize
	@IssueKeyPrefixPattern
	@NotBlank(message = "{valid.notblank}")
	String issueKeyPrefix
) {
}
