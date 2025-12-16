package com.tissue.issue.application.dto.request;

import lombok.Builder;

@Builder
public record SubscribeIssueCommand(
	String workspaceKey,
	String projectKey,
	String issueKey,
	Long actorMemberId
) {
}
