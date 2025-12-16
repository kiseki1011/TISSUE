package com.tissue.issue.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AssignParentIssueRequest(
	@NotBlank String parentProjectKey,
	@NotBlank String parentIssueKey
) {
}
