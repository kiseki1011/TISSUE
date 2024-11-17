package com.uranus.taskmanager.api.workspace.presentation.dto.response;

import com.uranus.taskmanager.api.workspace.domain.Workspace;

import lombok.Builder;
import lombok.Getter;

@Getter
public class WorkspaceCreateResponse {

	private final String code;
	private final String name;
	private final String description;

	@Builder
	public WorkspaceCreateResponse(String code, String name, String description) {
		this.code = code;
		this.name = name;
		this.description = description;
	}

	public static WorkspaceCreateResponse from(Workspace workspace) {
		return WorkspaceCreateResponse.builder()
			.name(workspace.getName())
			.description(workspace.getDescription())
			.code(workspace.getCode())
			.build();
	}
}
