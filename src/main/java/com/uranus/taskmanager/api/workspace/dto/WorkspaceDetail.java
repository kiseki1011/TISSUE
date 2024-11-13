package com.uranus.taskmanager.api.workspace.dto;

import java.time.LocalDateTime;

import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspacemember.WorkspaceRole;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public class WorkspaceDetail {

	/**
	 * Todo
	 *  - role 제거: role이 필요한 응답 DTO는 role 필드를 추가해서 사용하자
	 *  - WorkspaceDetail은 멤버에 상관없이 단순히 해당 워크스페이스에서 사용하는 일반 정보만을 나타내도록 하자
	 */

	private Long id;
	private String code;
	private String name;
	private String description;
	private int memberCount;
	private String createdBy;
	private LocalDateTime createdAt;
	private String updatedBy;
	private LocalDateTime updatedAt;
	private WorkspaceRole role;

	@Builder
	public WorkspaceDetail(Long id, String code, String name, String description, int memberCount, String createdBy,
		LocalDateTime createdAt, String updatedBy, LocalDateTime updatedAt, WorkspaceRole role) {
		this.id = id;
		this.code = code;
		this.name = name;
		this.description = description;
		this.memberCount = memberCount;
		this.createdBy = createdBy;
		this.createdAt = createdAt;
		this.updatedBy = updatedBy;
		this.updatedAt = updatedAt;
		this.role = role;
	}

	public static WorkspaceDetail from(Workspace workspace, WorkspaceRole role) {
		return WorkspaceDetail.builder()
			.id(workspace.getId())
			.code(workspace.getCode())
			.name(workspace.getName())
			.description(workspace.getDescription())
			.memberCount(workspace.getMemberCount())
			.createdBy(workspace.getCreatedBy())
			.createdAt(workspace.getCreatedDate())
			.updatedBy(workspace.getLastModifiedBy())
			.updatedAt(workspace.getLastModifiedDate())
			.role(role)
			.build();
	}
}
