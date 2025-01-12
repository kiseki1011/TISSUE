package com.tissue.api.workspacemember.domain;

import java.util.ArrayList;
import java.util.List;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.member.domain.Member;
import com.tissue.api.position.domain.Position;
import com.tissue.api.position.domain.WorkspaceMemberPosition;
import com.tissue.api.position.exception.DuplicatePositionAssignmentException;
import com.tissue.api.position.exception.PositionNotAssignedException;
import com.tissue.api.position.exception.PositionNotFoundException;
import com.tissue.api.security.authorization.exception.InsufficientWorkspaceRoleException;
import com.tissue.api.team.domain.Team;
import com.tissue.api.team.domain.WorkspaceMemberTeam;
import com.tissue.api.team.exception.DuplicateTeamAssignmentException;
import com.tissue.api.team.exception.TeamNotAssignedException;
import com.tissue.api.team.exception.TeamNotFoundException;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspacemember.exception.InvalidRoleUpdateException;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	uniqueConstraints = {
		@UniqueConstraint(
			name = "UK_WORKSPACE_NICKNAME",
			columnNames = {"WORKSPACE_CODE", "nickname"})
	}
)
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

	@Column(name = "WORKSPACE_CODE", nullable = false)
	private String workspaceCode;

	@OneToMany(mappedBy = "workspaceMember", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<WorkspaceMemberPosition> workspaceMemberPositions = new ArrayList<>();

	@OneToMany(mappedBy = "workspaceMember", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<WorkspaceMemberTeam> workspaceMemberTeams = new ArrayList<>();

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private WorkspaceRole role;

	@Column(nullable = false)
	private String nickname;

	@Builder
	public WorkspaceMember(
		Member member,
		Workspace workspace,
		WorkspaceRole role,
		String nickname
	) {
		this.member = member;
		this.workspace = workspace;
		this.role = role;
		this.nickname = nickname;
		this.workspaceCode = workspace.getCode();
	}

	public static WorkspaceMember addWorkspaceMember(
		Member member,
		Workspace workspace,
		WorkspaceRole role,
		String nickname
	) {
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

	public static WorkspaceMember addOwnerWorkspaceMember(
		Member member,
		Workspace workspace
	) {
		member.increaseMyWorkspaceCount();
		workspace.increaseMemberCount();
		return addWorkspaceMember(member, workspace, WorkspaceRole.OWNER, member.getEmail());
	}

	public static WorkspaceMember addCollaboratorWorkspaceMember(
		Member member,
		Workspace workspace
	) {
		workspace.increaseMemberCount();
		return addWorkspaceMember(member, workspace, WorkspaceRole.MEMBER, member.getEmail());
	}

	public void remove() {
		this.workspace.decreaseMemberCount();
		this.member.getWorkspaceMembers().remove(this);
		this.workspace.getWorkspaceMembers().remove(this);
	}

	public void addPosition(Position position) {
		validatePositionBelongsToWorkspace(position);
		validateDuplicateAssignedPosition(position);

		WorkspaceMemberPosition.builder()
			.workspaceMember(this)
			.position(position)
			.build();
	}

	public void removePosition(Position position) {
		WorkspaceMemberPosition workspaceMemberPosition = workspaceMemberPositions.stream()
			.filter(wmp -> wmp.getPosition().equals(position))
			.findFirst()
			.orElseThrow(() -> new PositionNotAssignedException(
				String.format("Position '%s' is not assigned to this workspace member", position.getName())
			));

		workspaceMemberPositions.remove(workspaceMemberPosition);
	}

	public void addTeam(Team team) {
		validateTeamBelongsToWorkspace(team);
		validateDuplicateAssignedTeam(team);

		WorkspaceMemberTeam.builder()
			.workspaceMember(this)
			.team(team)
			.build();
	}

	public void removeTeam(Team team) {
		WorkspaceMemberTeam workspaceMemberTeam = workspaceMemberTeams.stream()
			.filter(wmp -> wmp.getTeam().equals(team))
			.findFirst()
			.orElseThrow(() -> new TeamNotAssignedException(
				String.format("Team '%s' is not assigned to this workspace member", team.getName())
			));

		workspaceMemberTeams.remove(workspaceMemberTeam);
	}

	public void updateRole(WorkspaceRole role) {
		validateCannotUpdateToOwnerRole(role);
		this.role = role;
	}

	public void updateRoleFromOwnerToAdmin() {
		validateCurrentRoleIsOwner();
		updateRole(WorkspaceRole.ADMIN);
		this.member.decreaseMyWorkspaceCount();
	}

	public void updateRoleToOwner() {
		validateCurrentRoleIsNotOwner();
		this.role = WorkspaceRole.OWNER;
		this.member.increaseMyWorkspaceCount();
	}

	public void updateNickname(String nickname) {
		this.nickname = nickname;
	}

	public void validateRoleIsHigherThanViewer() {
		if (this.getRole().isLowerThan(WorkspaceRole.MEMBER)) {
			throw new InsufficientWorkspaceRoleException(
				String.format(
					"Must have a workspace role higher than VIEWER. Current workspace role: %s",
					this.getRole()
				)
			);
		}
	}

	private void validateDuplicateAssignedPosition(Position position) {
		boolean isAlreadyAssigned = this.workspaceMemberPositions.stream()
			.anyMatch(wmp -> wmp.getPosition().equals(position));

		if (isAlreadyAssigned) {
			throw new DuplicatePositionAssignmentException(
				String.format("Position '%s' is already assigned to this workspace member", position.getName())
			);
		}
	}

	private void validatePositionBelongsToWorkspace(Position position) {
		if (!position.getWorkspaceCode().equals(this.workspaceCode)) {
			throw new PositionNotFoundException();
		}
	}

	private void validateDuplicateAssignedTeam(Team team) {
		boolean isAlreadyAssigned = this.workspaceMemberTeams.stream()
			.anyMatch(wmt -> wmt.getTeam().equals(team));

		if (isAlreadyAssigned) {
			throw new DuplicateTeamAssignmentException(
				String.format("Team '%s' is already assigned to this workspace member", team.getName())
			);
		}
	}

	private void validateTeamBelongsToWorkspace(Team team) {
		if (!team.getWorkspaceCode().equals(this.workspaceCode)) {
			throw new TeamNotFoundException();
		}
	}

	private void validateCannotUpdateToOwnerRole(WorkspaceRole newRole) {
		if (newRole == WorkspaceRole.OWNER) {
			throw new InvalidRoleUpdateException(
				"You cannot directly change to OWNER role. Use ownership transfer instead.");
		}
	}

	private void validateCurrentRoleIsOwner() {
		if (this.role != WorkspaceRole.OWNER) {
			throw new InvalidRoleUpdateException("Current role must be OWNER.");
		}
	}

	private void validateCurrentRoleIsNotOwner() {
		if (this.role == WorkspaceRole.OWNER) {
			throw new InvalidRoleUpdateException("Current role cannot be OWNER.");
		}
	}
}
