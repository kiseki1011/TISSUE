package com.uranus.taskmanager.api.workspace.presentation.dto;

import java.time.LocalDateTime;

import com.uranus.taskmanager.api.workspace.domain.Workspace;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public class WorkspaceUpdateDetail {

	private String code;
	private String name;
	private String description;
	private String updatedBy;
	private LocalDateTime updatedAt;

	@Builder
	public WorkspaceUpdateDetail(String code, String name, String description, String updatedBy,
		LocalDateTime updatedAt) {
		this.code = code;
		this.name = name;
		this.description = description;
		this.updatedBy = updatedBy;
		this.updatedAt = updatedAt;
	}

	public static WorkspaceUpdateDetail from(Workspace workspace) {
		return WorkspaceUpdateDetail.builder()
			.code(workspace.getCode())
			.name(workspace.getName())
			.description(workspace.getDescription())
			.updatedBy(workspace.getLastModifiedBy())
			.updatedAt(workspace.getLastModifiedDate())
			.build();
	}
}
