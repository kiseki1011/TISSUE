package com.tissue.api.issue.base.application.dto;

public record RemoveParentIssueCommand(
	String workspaceCode,
	String issueKey
) {
}
