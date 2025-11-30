package com.tissue.api.project.application.dto.response;

import com.tissue.api.project.domain.ProjectMember;

public record ProjectMemberCommandResult(
	String workspaceKey,
	String projectKey,
	Long memberId
) {
	public static ProjectMemberCommandResult of(ProjectMember projectMember) {
		return new ProjectMemberCommandResult(
			projectMember.getWorkspaceKey(),
			projectMember.getProjectKey(),
			projectMember.getMemberId()
		);
	}
}
