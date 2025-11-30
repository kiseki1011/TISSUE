package com.tissue.api.issuetype.application.dto;

import lombok.Builder;

@Builder
public record DeleteIssueFieldCommand(
	String workspaceKey,
	String projectKey,
	Long issueTypeId,
	Long issueFieldId
) {
}
