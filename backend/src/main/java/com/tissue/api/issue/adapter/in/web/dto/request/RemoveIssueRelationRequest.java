package com.tissue.api.issue.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RemoveIssueRelationRequest(
	@NotBlank String targetProjectKey,
	@NotBlank String targetIssueKey
) {
}
