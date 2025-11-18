package com.tissue.api.project.domain;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.project.domain.enums.ProjectRole;
import com.tissue.api.workspace.domain.WorkspaceMember;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Entity
@Table(
	name = "project_member",
	uniqueConstraints = {
		@UniqueConstraint(columnNames = {"project_id", "workspace_member_id"})
	}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectMember extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "project_id", nullable = false)
	private Project project;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "workspace_member_id", nullable = false)
	private WorkspaceMember workspaceMember;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ProjectRole role;

	public static ProjectMember create(
		@NonNull Project project,
		@NonNull WorkspaceMember workspaceMember,
		@NonNull ProjectRole role
	) {
		ProjectMember projectMember = new ProjectMember();
		projectMember.project = project;
		projectMember.workspaceMember = workspaceMember;
		projectMember.role = role;
		return projectMember;
	}

	public void changeRole(@NonNull ProjectRole newRole) {
		this.role = newRole;
	}

	// TODO: workspaceKey는 불변이기 때문에 편의 필드로 추가하는 것을 고려
	public String getWorkspaceKey() {
		return project.getWorkspaceKey();
	}

	public String getProjectKey() {
		return project.getKey();
	}
}
