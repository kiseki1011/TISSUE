package com.tissue.api.issue.presentation.dto.request;

import com.tissue.api.issue.application.dto.PerformTransitionCommand;

import jakarta.validation.constraints.NotNull;

public record PerformTransitionRequest(
	@NotNull Long transitionId
) {
	public PerformTransitionCommand toCommand(String workspaceKey, String issueKey, Long actorMemberId) {
		return PerformTransitionCommand.builder()
			.workspaceKey(workspaceKey)
			.issueKey(issueKey)
			.actorMemberId(actorMemberId)
			.transitionId(transitionId)
			.build();
	}
}
