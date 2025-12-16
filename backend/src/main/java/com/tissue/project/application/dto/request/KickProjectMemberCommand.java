package com.tissue.project.application.dto.request;

import lombok.Builder;

@Builder
public record KickProjectMemberCommand(
	String workspaceKey,
	String projectKey,
	Long targetMemberId,
	Long actorMemberId
) {
}
