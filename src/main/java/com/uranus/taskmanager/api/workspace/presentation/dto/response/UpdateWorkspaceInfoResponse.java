package com.uranus.taskmanager.api.workspace.presentation.dto.response;

import java.time.LocalDateTime;

import com.uranus.taskmanager.api.workspace.domain.Workspace;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public class UpdateWorkspaceInfoResponse {

	private Long id;
	private LocalDateTime updatedAt;
	private String updatedBy;
	private String code;
	private String name;
	private String description;

	@Builder
	public UpdateWorkspaceInfoResponse(
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

	public static UpdateWorkspaceInfoResponse from(Workspace workspace) {
		return UpdateWorkspaceInfoResponse.builder()
			.id(workspace.getId())
			.updatedAt(workspace.getLastModifiedDate())
			.updatedBy(workspace.getLastModifiedBy())
			.code(workspace.getCode())
			.name(workspace.getName())
			.description(workspace.getDescription())
			.build();
	}
}
