package com.uranus.taskmanager.api.workspace.dto.response;

import com.uranus.taskmanager.api.workspace.domain.Workspace;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@Builder
public class WorkspaceResponse {

	private final String workspaceCode;
	private final String name;
	private final String description;

	public static WorkspaceResponse fromEntity(Workspace workspace) {
		return WorkspaceResponse.builder()
			.name(workspace.getName())
			.description(workspace.getDescription())
			.workspaceCode(workspace.getWorkspaceCode())
			.build();
	}
}
