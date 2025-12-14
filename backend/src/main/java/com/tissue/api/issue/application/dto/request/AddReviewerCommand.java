package com.tissue.api.issue.application.dto.request;

import lombok.Builder;

@Builder
public record AddReviewerCommand(
	String workspaceKey,
	String projectKey,
	String issueKey,
	Long targetMemberId,
	Long actorMemberId
) {
}
