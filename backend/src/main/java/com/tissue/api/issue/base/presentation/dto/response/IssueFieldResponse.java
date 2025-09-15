package com.tissue.api.issue.base.presentation.dto.response;

import com.tissue.api.issue.base.domain.model.IssueField;

import lombok.Builder;

@Builder
public record IssueFieldResponse(
	String workspaceCode,
	Long issueTypeId,
	Long issueFieldId
) {
	public static IssueFieldResponse from(IssueField issueField) {
		return IssueFieldResponse.builder()
			.workspaceCode(issueField.getWorkspaceCode())
			.issueTypeId(issueField.getIssueType().getId())
			.issueFieldId(issueField.getId())
			.build();
	}
}
