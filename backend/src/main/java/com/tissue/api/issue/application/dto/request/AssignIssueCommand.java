package com.tissue.api.issue.application.dto.request;

import lombok.Builder;

@Builder
public record AssignIssueCommand(
	String workspaceKey,
	String projectKey,
	String issueKey,
	Long memberId
) {
}
