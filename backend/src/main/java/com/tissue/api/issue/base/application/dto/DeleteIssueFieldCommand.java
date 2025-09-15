package com.tissue.api.issue.base.application.dto;

public record DeleteIssueFieldCommand(
	String workspaceKey,
	Long issueTypeId,
	Long issueFieldId
) {
}
