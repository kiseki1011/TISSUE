package com.uranus.taskmanager.api.response;

import com.uranus.taskmanager.api.domain.workspace.Workspace;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class WorkspaceResponse {

	private final Long id;
	private final String workspaceId;
	private final String name;
	private final String description;

	//    private final LocalDateTime createdAt;
	//    private final LocalDateTime updatedAt;
	//    private final LocalDateTime viewedAt;

	@Builder
	public WorkspaceResponse(Long id, String workspaceId, String name, String description) {
		this.id = id;
		this.workspaceId = workspaceId;
		this.name = name;
		this.description = description;
	}

	public static WorkspaceResponse fromEntity(Workspace workspace) {
		return WorkspaceResponse.builder()
			.id(workspace.getId())
			.name(workspace.getName())
			.description(workspace.getDescription())
			.workspaceId(workspace.getWorkspaceId())
			.build();
	}

}
