package com.tissue.api.issue.application.dto;

public record IssueCountStats(
	long totalCount,
	long doneCount
) {
}
