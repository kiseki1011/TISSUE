package com.tissue.api.workspace.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateIssueKeyRequest(
	@NotBlank(message = "Issue key must not be blank.")
	@Pattern(regexp = "^[a-zA-Z]+$", message = "Must contain only alphabetic characters.")
	@Size(min = 3, max = 16, message = "A issue key must between 3 ~ 16 characters long.")
	String issueKeyPrefix
) {
}
