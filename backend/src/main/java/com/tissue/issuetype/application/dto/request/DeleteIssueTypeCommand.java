package com.tissue.issuetype.application.dto.request;

public record DeleteIssueTypeCommand(
	String workspaceKey,
	String projectKey,
	Long id
) {
}
