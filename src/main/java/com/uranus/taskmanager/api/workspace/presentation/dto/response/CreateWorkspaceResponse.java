package com.uranus.taskmanager.api.workspace.presentation.dto.response;

import java.time.LocalDateTime;

import com.uranus.taskmanager.api.workspace.domain.Workspace;

import lombok.Builder;
import lombok.Getter;

@Getter
public class CreateWorkspaceResponse {

	private Long id;
	private LocalDateTime createdAt;
	private String createdBy;
	private String code;
	private String name;
	private String description;

	@Builder
	public CreateWorkspaceResponse(
		Long id,
		LocalDateTime createdAt,
		String createdBy,
		String code,
		String name,
		String description
	) {
		this.id = id;
		this.createdAt = createdAt;
		this.createdBy = createdBy;
		this.code = code;
		this.name = name;
		this.description = description;
	}

	public static CreateWorkspaceResponse from(Workspace workspace) {
		return CreateWorkspaceResponse.builder()
			.id(workspace.getId())
			.createdAt(workspace.getCreatedDate())
			.createdBy(workspace.getCreatedBy())
			.code(workspace.getCode())
			.name(workspace.getName())
			.description(workspace.getDescription())
			.build();
	}
}
