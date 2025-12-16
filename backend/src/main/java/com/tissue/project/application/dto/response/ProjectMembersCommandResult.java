package com.tissue.project.application.dto.response;

import java.util.Collection;
import java.util.List;

import com.tissue.project.domain.Project;
import com.tissue.project.domain.ProjectMember;

public record ProjectMembersCommandResult(
	String workspaceKey,
	String projectKey,
	List<Long> memberIds,
	int totalSize
) {
	public static ProjectMembersCommandResult of(Project project, Collection<ProjectMember> projectMembers) {
		List<Long> ids = projectMembers.stream()
			.map(ProjectMember::getMemberId)
			.toList();

		return new ProjectMembersCommandResult(
			project.getWorkspaceKey(),
			project.getKey(),
			ids,
			ids.size()
		);
	}
}
