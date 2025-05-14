package com.tissue.api.issue.presentation.controller.dto.request;

import com.tissue.api.issue.domain.model.enums.IssueRelationType;

import jakarta.validation.constraints.NotNull;

public record CreateIssueRelationRequest(
	@NotNull(message = "{valid.notnull}")
	IssueRelationType relationType
) {
}
