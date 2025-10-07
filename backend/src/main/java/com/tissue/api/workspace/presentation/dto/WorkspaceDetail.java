package com.tissue.api.workspace.presentation.dto;

import java.time.Instant;

import com.tissue.api.workspace.domain.model.Workspace;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public class WorkspaceDetail {

	private Long id;
	private String code;
	private String name;
	private String description;
	private int memberCount;
	private Long createdBy;
	private Instant createdAt;
	private Long updatedBy;
	private Instant updatedAt;

	@Builder
	public WorkspaceDetail(
		Long id,
		String code,
		String name,
		String description,
		int memberCount,
		Long createdBy,
		Instant createdAt,
		Long updatedBy,
		Instant updatedAt
	) {
		this.id = id;
		this.code = code;
		this.name = name;
		this.description = description;
		this.memberCount = memberCount;
		this.createdBy = createdBy;
		this.createdAt = createdAt;
		this.updatedBy = updatedBy;
		this.updatedAt = updatedAt;
	}

	public static WorkspaceDetail from(Workspace workspace) {
		return WorkspaceDetail.builder()
			.id(workspace.getId())
			.code(workspace.getKey())
			.name(workspace.getName())
			.description(workspace.getDescription())
			.memberCount(workspace.getMemberCount())
			.createdBy(workspace.getCreatedBy())
			.createdAt(workspace.getCreatedAt())
			.updatedBy(workspace.getLastModifiedBy())
			.updatedAt(workspace.getLastModifiedAt())
			.build();
	}
}
