package com.uranus.taskmanager.api.workspacemember.domain;

import com.uranus.taskmanager.api.common.entity.BaseEntity;
import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspacemember.WorkspaceRole;

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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkspaceMember extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MEMBER_ID", nullable = false)
	private Member member;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "WORKSPACE_ID", nullable = false)
	private Workspace workspace;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private WorkspaceRole role;

	@Column(nullable = false)
	private String nickname;

	@Builder
	public WorkspaceMember(Member member, Workspace workspace, WorkspaceRole role, String nickname) {
		this.member = member;
		this.workspace = workspace;
		this.role = role;
		this.nickname = nickname;
	}

	public static WorkspaceMember addWorkspaceMember(Member member, Workspace workspace, WorkspaceRole role,
		String nickname) {
		WorkspaceMember workspaceMember = WorkspaceMember.builder()
			.member(member)
			.workspace(workspace)
			.role(role)
			.nickname(nickname)
			.build();

		member.getWorkspaceMembers().add(workspaceMember);
		workspace.getWorkspaceMembers().add(workspaceMember);

		return workspaceMember;
	}

	public static WorkspaceMember addOwnerWorkspaceMember(Member member, Workspace workspace) {
		member.increaseMyWorkspaceCount();
		workspace.increaseMemberCount();
		return addWorkspaceMember(member, workspace, WorkspaceRole.OWNER, member.getEmail());
	}

	public static WorkspaceMember addCollaboratorWorkspaceMember(Member member, Workspace workspace) {
		workspace.increaseMemberCount();
		return addWorkspaceMember(member, workspace, WorkspaceRole.COLLABORATOR, member.getEmail());
	}

	public void remove() {
		this.workspace.decreaseMemberCount();
		this.member.getWorkspaceMembers().remove(this);
		this.workspace.getWorkspaceMembers().remove(this);
	}

	public void updateRole(WorkspaceRole role) {
		this.role = role;
	}

	public void updateRoleFromOwnerToManager() {
		updateRole(WorkspaceRole.MANAGER);
		this.member.decreaseMyWorkspaceCount();
	}

	public void updateRoleToOwner() {
		updateRole(WorkspaceRole.OWNER);
		this.member.increaseMyWorkspaceCount();
	}
}
