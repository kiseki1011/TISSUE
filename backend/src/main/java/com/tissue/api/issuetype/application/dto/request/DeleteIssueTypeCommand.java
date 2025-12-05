package com.tissue.api.issuetype.application.dto.request;

public record DeleteIssueTypeCommand(
	String workspaceKey,
	String projectKey,
	Long id
) {
}
