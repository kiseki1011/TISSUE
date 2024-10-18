package com.uranus.taskmanager.api.workspace.dto.response;

import com.uranus.taskmanager.api.workspace.domain.Workspace;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@Builder
public class WorkspaceCreateResponse {

	private final String code;
	private final String name;
	private final String description;

	public static WorkspaceCreateResponse from(Workspace workspace) {
		return WorkspaceCreateResponse.builder()
			.name(workspace.getName())
			.description(workspace.getDescription())
			.code(workspace.getCode())
			.build();
	}
}
