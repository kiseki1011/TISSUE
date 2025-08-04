package com.tissue.api.issue.base.presentation.dto.response;

import com.tissue.api.issue.base.domain.model.IssueField;

import lombok.Builder;

@Builder
public record IssueFieldResponse(
	String workspaceCode,
	String issueTypeKey,
	String issueFieldKey,
	String issueFieldFieldKey
) {
	// TODO: Use Join Fetch to solve additional query
	public static IssueFieldResponse from(IssueField issueField) {
		return IssueFieldResponse.builder()
			.workspaceCode(issueField.getWorkspaceCode())
			.issueTypeKey(issueField.getIssueType().getKey())
			.issueFieldKey(issueField.getKey())
			.build();
	}
}
