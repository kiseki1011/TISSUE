package com.tissue.api.issue.application.dto;

public record IssueCountProjection(
	Long stateId,
	Long count
) {
}
