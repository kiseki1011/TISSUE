package com.tissue.api.issue.application.dto;

import lombok.Builder;

@Builder
public record PerformTransitionCommand(
	String workspaceKey,
	String issueKey,
	Long transitionId,
	Long actorMemberId
) {
}
