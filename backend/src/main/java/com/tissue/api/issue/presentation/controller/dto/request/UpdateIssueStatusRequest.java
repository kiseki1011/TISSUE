package com.tissue.api.issue.presentation.controller.dto.request;

import com.tissue.api.issue.domain.model.enums.IssueStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateIssueStatusRequest(
	@NotNull(message = "{valid.notnull}")
	IssueStatus status
) {
}
