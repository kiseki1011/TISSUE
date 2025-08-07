package com.tissue.api.workspacemember.domain.model;

import java.util.HashSet;
import java.util.Set;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.member.domain.model.Member;
import com.tissue.api.position.domain.model.Position;
import com.tissue.api.team.domain.model.Team;
import com.tissue.api.workspace.domain.model.Workspace;
import com.tissue.api.workspacemember.domain.model.enums.WorkspaceRole;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
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

	// TODO: 양방향 관계를 사용하는게 좋나? 아니면 단방향을 사용하는게 더 좋은가?
	@OneToMany(mappedBy = "workspaceMember", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<WorkspaceMemberPosition> workspaceMemberPositions = new HashSet<>();

	@OneToMany(mappedBy = "workspaceMember", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<WorkspaceMemberTeam> workspaceMemberTeams = new HashSet<>();

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private WorkspaceRole role;

	@Column(nullable = false)
	private String displayName;

	@Column(nullable = false)
	private String email;

	//  TODO: consider using a combined value of displayName, username(member's username) to use at the client
	//   example1: displayName + member.getUsername()
	//   example2: Make a VO called DisplayUsername
	// private String displayWithUsername;

	@Builder
	public WorkspaceMember(
		Member member,
		Workspace workspace,
		WorkspaceRole role
	) {
		this.member = member;
		this.workspace = workspace;
		this.role = role;
		this.displayName = member.getUsername();
		this.email = member.getEmail();
	}

	// TODO: Should i make this private?
	// TODO: Should i make this a instance method?
	public static WorkspaceMember createWorkspaceMember(
		Member member,
		Workspace workspace,
		WorkspaceRole role
	) {
		WorkspaceMember workspaceMember = WorkspaceMember.builder()
			.member(member)
			.workspace(workspace)
			.role(role)
			.build();

		member.getWorkspaceMembers().add(workspaceMember);
		workspace.getWorkspaceMembers().add(workspaceMember);

		member.validateWorkspaceLimit();
		workspace.validateMemberLimit();

		return workspaceMember;
	}

	public static WorkspaceMember addOwnerWorkspaceMember(
		Member member,
		Workspace workspace
	) {
		return createWorkspaceMember(member, workspace, WorkspaceRole.OWNER);
	}

	public static WorkspaceMember addWorkspaceMember(
		Member member,
		Workspace workspace
	) {
		return createWorkspaceMember(member, workspace, WorkspaceRole.MEMBER);
	}

	public String getWorkspaceKey() {
		return workspace.getKey();
	}

	public Long getMemberId() {
		return member.getId();
	}

	// TODO: For WorkspaceMember removal should i use hard-delete?
	//  Im thinking about what would happen to exisiting resources (Issue, Sprint, Comment, etc...)
	//  if the WorkspaceMember is kicked out of the Workspace.

	public void validateCanLeaveWorkspace() {
		if (this.role == WorkspaceRole.OWNER) {
			throw new InvalidOperationException("Cannot leave workspace if OWNER.");
		}
	}

	public void remove() {
		this.member.getWorkspaceMembers().remove(this);
		this.workspace.getWorkspaceMembers().remove(this);
	}

	public void updateRole(WorkspaceRole role) {
		validateUpdateToOwnerRole(role);
		this.role = role;
	}

	public void updateRoleToAdmin() {
		validateCurrentRoleIsOwner();
		updateRole(WorkspaceRole.ADMIN);
	}

	public void updateRoleToOwner() {
		validateCurrentRoleIsNotOwner();
		this.role = WorkspaceRole.OWNER;
	}

	public void updateDisplayName(String displayName) {
		this.displayName = displayName;
	}

	// TODO: Should this be WorkspaceMember's responsibility? Or WorkspaceRole enum's responsibility?
	public boolean roleIsHigherThan(WorkspaceRole role) {
		return this.role.isHigherThan(role);
	}

	public boolean roleIsLowerThan(WorkspaceRole role) {
		return this.role.isLowerThan(role);
	}

	private void validateUpdateToOwnerRole(WorkspaceRole newRole) {
		if (newRole == WorkspaceRole.OWNER) {
			throw new InvalidOperationException("Cannot directly change to OWNER role. Use ownership transfer.");
		}
	}

	private void validateCurrentRoleIsOwner() {
		if (this.role != WorkspaceRole.OWNER) {
			throw new InvalidOperationException("Current role must be OWNER.");
		}
	}

	private void validateCurrentRoleIsNotOwner() {
		if (this.role == WorkspaceRole.OWNER) {
			throw new InvalidOperationException("Current role cannot be OWNER.");
		}
	}

	public void addPosition(Position position) {
		this.workspaceMemberPositions.add(new WorkspaceMemberPosition(this, position));
	}

	public void removePosition(Position position) {
		this.workspaceMemberPositions.removeIf(wmp -> wmp.getPosition().equals(position));
	}

	public void addTeam(Team team) {
		this.workspaceMemberTeams.add(new WorkspaceMemberTeam(this, team));
	}

	public void removeTeam(Team team) {
		this.workspaceMemberTeams.removeIf(wmp -> wmp.getTeam().equals(team));
	}
}
