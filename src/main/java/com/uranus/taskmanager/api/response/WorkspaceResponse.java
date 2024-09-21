package com.uranus.taskmanager.api.response;

import com.uranus.taskmanager.api.domain.workspace.Workspace;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@Builder
public class WorkspaceResponse {

	private final Long id;
	private final String workspaceCode;
	private final String name;
	private final String description;

	public static WorkspaceResponse fromEntity(Workspace workspace) {
		return WorkspaceResponse.builder()
			.id(workspace.getId())
			.name(workspace.getName())
			.description(workspace.getDescription())
			.workspaceCode(workspace.getWorkspaceCode())
			.build();
	}

}
