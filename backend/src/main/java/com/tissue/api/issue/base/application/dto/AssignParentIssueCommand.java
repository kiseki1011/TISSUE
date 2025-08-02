package com.tissue.api.issue.base.application.dto;

public record AssignParentIssueCommand(
	String workspaceCode,
	String childIssueKey,
	String parentIssueKey
) {
}
