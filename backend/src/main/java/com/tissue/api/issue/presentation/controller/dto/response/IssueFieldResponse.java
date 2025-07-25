package com.tissue.api.issue.presentation.controller.dto.response;

import com.tissue.api.issue.domain.newmodel.IssueFieldDefinition;

import lombok.Builder;

@Builder
public record IssueFieldResponse(
	String workspaceCode,
	String issueTypeKey,
	String issueFieldKey,
	String issueFieldFieldKey
) {
	public static IssueFieldResponse from(
		String workspaceCode,
		String issueTypeKey,
		IssueFieldDefinition issueFieldDefinition
	) {
		return IssueFieldResponse.builder()
			.workspaceCode(workspaceCode)
			.issueTypeKey(issueTypeKey)
			.issueFieldKey(issueFieldDefinition.getKey())
			.issueFieldFieldKey(issueFieldDefinition.getFieldKey())
			.build();
	}
}
