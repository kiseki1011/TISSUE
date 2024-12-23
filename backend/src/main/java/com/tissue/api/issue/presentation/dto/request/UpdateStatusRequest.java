package com.tissue.api.issue.presentation.dto.request;

import com.tissue.api.issue.domain.enums.IssueStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(
	@NotNull
	IssueStatus status
) {
}
