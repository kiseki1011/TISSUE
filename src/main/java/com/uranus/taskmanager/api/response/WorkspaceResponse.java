package com.uranus.taskmanager.api.response;

import com.uranus.taskmanager.api.domain.workspace.Workspace;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class WorkspaceResponse {

	private final Long id;
	private final String name;
	private final String description;
	private final String workspaceCode;
	//    private final LocalDateTime createdAt;
	//    private final LocalDateTime updatedAt;
	//    private final LocalDateTime viewedAt;

	@Builder
	public WorkspaceResponse(Long id, String name, String description, String workspaceCode) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.workspaceCode = workspaceCode;
	}

	public static WorkspaceResponse fromEntity(Workspace workspace) {
		return WorkspaceResponse.builder()
			.id(workspace.getId())
			.name(workspace.getName())
			.description(workspace.getDescription())
			.workspaceCode(workspace.getWorkspaceCode())
			.build();
	}

}
