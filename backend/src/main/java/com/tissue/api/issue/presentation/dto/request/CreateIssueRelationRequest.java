package com.tissue.api.issue.presentation.dto.request;

import com.tissue.api.issue.domain.enums.IssueRelationType;

import jakarta.validation.constraints.NotNull;

public record CreateIssueRelationRequest(
	@NotNull(message = "Relation type is required.")
	IssueRelationType relationType
) {
}
