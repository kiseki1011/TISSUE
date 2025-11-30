package com.tissue.api.issuetype.application.dto;

public record DeleteIssueTypeCommand(
	String workspaceKey,
	String projectKey,
	Long id
) {
}
