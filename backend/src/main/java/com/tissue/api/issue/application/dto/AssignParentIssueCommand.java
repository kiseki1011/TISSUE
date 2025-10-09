package com.tissue.api.issue.application.dto;

public record AssignParentIssueCommand(
	String workspaceCode,
	String childIssueKey,
	String parentIssueKey
) {
}
