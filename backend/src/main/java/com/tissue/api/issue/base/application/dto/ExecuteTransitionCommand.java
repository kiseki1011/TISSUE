package com.tissue.api.issue.base.application.dto;

public record ExecuteTransitionCommand(
	String workspaceKey,
	String issueKey,
	Long transitionId,
	Long actorMemberId
) {
}
