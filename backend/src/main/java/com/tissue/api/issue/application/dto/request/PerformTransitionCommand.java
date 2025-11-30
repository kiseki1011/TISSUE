package com.tissue.api.issue.application.dto.request;

import lombok.Builder;

@Builder
public record PerformTransitionCommand(
	String workspaceKey,
	String projectKey,
	String issueKey,
	Long transitionId,
	Long actorMemberId
) {
}
