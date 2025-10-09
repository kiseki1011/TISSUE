package com.tissue.api.issue.application.dto;

public record DeleteIssueCommand(
	String workspaceCode,
	String issueKey
) {
}
