package com.tissue.api.issue.presentation.controller.dto.response;

import com.tissue.api.issue.domain.newmodel.IssueTypeDefinition;

public record IssueTypeResponse(
	String workspaceCode,
	String issueTypeKey
) {
	// TODO: Should i just use issueTypeDefinition.getWorkspace().getCode()?
	//  - Do i have to solve N+1 problems using join fetch?
	public static IssueTypeResponse from(String workspaceCode, IssueTypeDefinition issueTypeDefinition) {
		return new IssueTypeResponse(workspaceCode, issueTypeDefinition.getKey());
	}
}
