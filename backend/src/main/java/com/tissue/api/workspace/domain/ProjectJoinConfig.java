package com.tissue.api.workspace.domain;

import com.tissue.api.project.domain.Project;
import com.tissue.api.project.domain.enums.ProjectRole;

// TODO: workspace에서 project 패키지로 이동시킬까?
public record ProjectJoinConfig(
	Long projectId,
	String projectKey,
	ProjectRole role
) {
	public static ProjectJoinConfig of(Project project, ProjectRole role) {
		return new ProjectJoinConfig(project.getId(), project.getKey(), role);
	}
}
