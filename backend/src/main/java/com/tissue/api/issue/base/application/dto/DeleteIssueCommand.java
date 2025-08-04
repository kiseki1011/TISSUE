package com.tissue.api.issue.base.application.dto;

public record DeleteIssueCommand(
	String workspaceCode,
	String issueKey
) {
}
