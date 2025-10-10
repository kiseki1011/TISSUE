package com.tissue.api.issue.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RemoveIssueRelationRequest(
	@NotBlank String targetIssueKey
) {
}
