package com.tissue.api.issue.application.dto;

public record RemoveParentIssueCommand(
	String workspaceCode,
	String issueKey
) {
}
