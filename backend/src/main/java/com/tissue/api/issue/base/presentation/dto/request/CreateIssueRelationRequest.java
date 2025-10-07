package com.tissue.api.issue.base.presentation.dto.request;

import com.tissue.api.issue.base.domain.enums.IssueRelationType;

import jakarta.validation.constraints.NotNull;

public record CreateIssueRelationRequest(
	@NotNull(message = "{valid.notnull}")
	IssueRelationType relationType
) {
}
