package com.tissue.api.issue.application.dto;

public record ExecuteTransitionCommand(
	String workspaceKey,
	String issueKey,
	Long transitionId,
	Long actorMemberId
) {
}
