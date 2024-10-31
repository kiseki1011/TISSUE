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

	private Long id;
	private String code;
	private String name;
	private String description;
	private String createdBy;
	private LocalDateTime createdAt;
	private WorkspaceRole role;

	/*
	 * Todo
	 *  - Workspace에 headcount 필드 추가
	 *  - Workspace에 increaseHeadcount(), decreaseHeadcount() 추가
	 *  - WorkspaceMember에 removeWorkspaceMember() 추가
	 *  - headcount 필드 추가에 따라 WorkspaceService의 participateWorkspace()를 수정한다
	 *  - 이후 WorkspaceDetail의 headcount 사용
	 */
	// private int headcount;

	@Builder
	public WorkspaceDetail(Long id, String code, String name, String description, String createdBy,
		LocalDateTime createdAt, WorkspaceRole role) {
		this.id = id;
		this.code = code;
		this.name = name;
		this.description = description;
		this.createdBy = createdBy;
		this.createdAt = createdAt;
		this.role = role;
	}

	public static WorkspaceDetail from(Workspace workspace, WorkspaceRole role) {
		return WorkspaceDetail.builder()
			.id(workspace.getId())
			.code(workspace.getCode())
			.name(workspace.getName())
			.description(workspace.getDescription())
			.createdBy(workspace.getCreatedBy())
			.createdAt(workspace.getCreatedDate())
			.role(role)
			.build();
	}
}
