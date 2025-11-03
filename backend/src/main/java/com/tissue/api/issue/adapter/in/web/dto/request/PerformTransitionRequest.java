package com.tissue.api.issue.adapter.in.web.dto.request;

import com.tissue.api.issue.application.dto.request.PerformTransitionCommand;

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
