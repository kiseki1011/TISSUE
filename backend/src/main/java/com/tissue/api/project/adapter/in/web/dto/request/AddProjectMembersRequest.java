package com.tissue.api.project.adapter.in.web.dto.request;

import java.util.List;

import com.tissue.api.project.application.dto.request.AddProjectMembersCommand;
import com.tissue.api.project.domain.enums.ProjectRole;

import jakarta.validation.constraints.NotNull;

public record AddProjectMembersRequest(
	List<MemberRequestConfig> members
) {
	public record MemberRequestConfig(
		@NotNull Long memberId,
		@NotNull ProjectRole role
	) {
	}

	public AddProjectMembersCommand toCommand(String workspaceKey, String projectKey) {
		List<AddProjectMembersCommand.ProjectMemberConfig> configs = members.stream()
			.map(m -> new AddProjectMembersCommand.ProjectMemberConfig(
				m.memberId(),
				m.role()
			))
			.toList();

		return new AddProjectMembersCommand(workspaceKey, projectKey, configs);
	}
}
