package com.uranus.taskmanager.api.workspace.presentation.dto.response;

import java.time.LocalDateTime;

import com.uranus.taskmanager.api.workspace.domain.Workspace;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public class UpdateWorkspaceResponse {

	private Long id;
	private LocalDateTime updatedAt;
	private String updatedBy;
	private String code;
	private String name;
	private String description;

	@Builder
	public UpdateWorkspaceResponse(
		Long id,
		LocalDateTime updatedAt,
		String updatedBy,
		String code,
		String name,
		String description
	) {
		this.id = id;
		this.updatedAt = updatedAt;
		this.updatedBy = updatedBy;
		this.code = code;
		this.name = name;
		this.description = description;
	}

	public static UpdateWorkspaceResponse from(Workspace workspace) {
		return UpdateWorkspaceResponse.builder()
			.id(workspace.getId())
			.updatedAt(workspace.getLastModifiedDate())
			.updatedBy(workspace.getLastModifiedBy())
			.code(workspace.getCode())
			.name(workspace.getName())
			.description(workspace.getDescription())
			.build();
	}
}
