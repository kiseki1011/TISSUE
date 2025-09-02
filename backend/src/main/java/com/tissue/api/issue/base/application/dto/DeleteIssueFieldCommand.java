package com.tissue.api.issue.base.application.dto;

public record DeleteIssueFieldCommand(
	String workspaceKey,
	String issueTypeKey,
	String issueFieldKey
) {
}
