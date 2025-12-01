package com.tissue.api.workspace.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.lang.Nullable;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.project.domain.Project;
import com.tissue.api.project.domain.enums.ProjectRole;
import com.tissue.api.workspace.domain.converter.ProjectJoinConfigListConverter;
import com.tissue.api.workspace.domain.enums.WorkspaceRole;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkspaceInviteLink extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String token; // URL용 랜덤 토큰 (UUID 등)

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "workspace_id", nullable = false)
	private Workspace workspace;

	private String workspaceKey;

	@Column(nullable = false)
	private boolean active; // 파기 여부

	@Column(nullable = true)
	private Instant expiredAt; // 만료일 (null이면 무제한)

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private WorkspaceRole workspaceRole;

	@Convert(converter = ProjectJoinConfigListConverter.class)
	@Column(name = "project_configs", columnDefinition = "jsonb")
	private List<ProjectJoinConfig> projectConfigs = new ArrayList<>();

	public static WorkspaceInviteLink create(
		@NonNull Workspace workspace,
		@NonNull String token,
		@Nullable WorkspaceRole role,
		@Nullable Instant expiredAt
	) {
		WorkspaceInviteLink link = new WorkspaceInviteLink();
		link.workspace = workspace;
		link.workspaceKey = workspace.getKey();
		link.token = token;
		link.workspaceRole = role != null ? role : WorkspaceRole.MEMBER;
		link.active = true;
		link.expiredAt = expiredAt;

		return link;
	}

	public void addProjectConfig(Project project, ProjectRole role) {
		this.projectConfigs.add(ProjectJoinConfig.of(project, role));
	}

	public boolean isValid() {
		if (!active) {
			return false;
		}
		return isPermanentLink() || isNotExpired();
	}

	public boolean isPermanentLink() {
		return expiredAt == null;
	}

	public boolean isExpired() {
		return Instant.now().isAfter(expiredAt);
	}

	public boolean isNotExpired() {
		return !isExpired();
	}

	public void expire() {
		this.active = false;
	}
}
