package com.tissue.api.issuetype.presentation.dto.response;

import com.tissue.api.issuetype.domain.IssueField;

import lombok.Builder;

@Builder
public record IssueFieldResponse(
	String workspaceKey,
	Long issueTypeId,
	Long issueFieldId
) {
	public static IssueFieldResponse from(IssueField issueField) {
		return IssueFieldResponse.builder()
			.workspaceKey(issueField.getWorkspaceKey())
			.issueTypeId(issueField.getIssueType().getId())
			.issueFieldId(issueField.getId())
			.build();
	}
}
