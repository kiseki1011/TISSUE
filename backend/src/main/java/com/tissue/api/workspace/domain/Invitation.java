package com.tissue.api.workspace.domain;

import static com.tissue.api.workspace.domain.enums.InvitationStatus.*;

import java.util.ArrayList;
import java.util.List;

import org.springframework.lang.Nullable;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.member.domain.model.Member;
import com.tissue.api.project.domain.Project;
import com.tissue.api.project.domain.enums.ProjectRole;
import com.tissue.api.workspace.domain.converter.ProjectJoinConfigListConverter;
import com.tissue.api.workspace.domain.enums.InvitationStatus;
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
public class Invitation extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "workspace_id", nullable = false)
	private Workspace workspace;

	@Column(name = "workspace_key", nullable = false)
	private String workspaceKey;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private InvitationStatus status;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private WorkspaceRole workspaceRole;

	@Convert(converter = ProjectJoinConfigListConverter.class)
	@Column(name = "project_configs", columnDefinition = "jsonb")
	private List<ProjectJoinConfig> projectConfigs = new ArrayList<>();

	public static Invitation create(
		@NonNull Workspace workspace,
		@NonNull Member member,
		@Nullable WorkspaceRole workspaceRole
	) {
		Invitation invitation = new Invitation();
		invitation.member = member;
		invitation.workspace = workspace;
		invitation.workspaceKey = workspace.getKey();
		invitation.status = PENDING;
		invitation.workspaceRole = (workspaceRole != null) ? workspaceRole : WorkspaceRole.MEMBER;

		return invitation;
	}

	public void addProjectConfig(Project project, ProjectRole role) {
		this.projectConfigs.add(ProjectJoinConfig.of(project, role));
	}

	public void accept() {
		this.status = ACCEPTED;
	}

	public void reject() {
		this.status = REJECTED;
	}

	public boolean isProcessed() {
		return !isPending();
	}

	public boolean isPending() {
		return this.status == PENDING;
	}

	public boolean projectConfigsNotEmpty() {
		return !projectConfigs.isEmpty();
	}
}
