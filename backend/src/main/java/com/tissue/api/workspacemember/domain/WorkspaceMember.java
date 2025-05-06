package com.tissue.api.workspacemember.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.common.exception.type.ForbiddenOperationException;
import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.member.domain.Member;
import com.tissue.api.position.domain.Position;
import com.tissue.api.position.domain.WorkspaceMemberPosition;
import com.tissue.api.team.domain.Team;
import com.tissue.api.team.domain.WorkspaceMemberTeam;
import com.tissue.api.workspace.domain.Workspace;

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
	private Set<WorkspaceMemberPosition> workspaceMemberPositions = new HashSet<>();

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

	// TODO: increaseMyWorkspaceCount, decreaseMyWorkspaceCount 호출은 어디서 하는 것이 제일 좋을까?
	public static WorkspaceMember addOwnerWorkspaceMember(
		Member member,
		Workspace workspace,
		String nickname
	) {
		member.increaseMyWorkspaceCount();
		workspace.increaseMemberCount();
		return addWorkspaceMember(member, workspace, WorkspaceRole.OWNER, nickname);
	}

	public static WorkspaceMember addMemberWorkspaceMember(
		Member member,
		Workspace workspace,
		String nickname
	) {
		workspace.increaseMemberCount();
		return addWorkspaceMember(member, workspace, WorkspaceRole.MEMBER, nickname);
	}

	// @Deprecated
	// public void removeFromWorkspace() {
	// 	boolean notDeleted = !this.isDeleted();
	//
	// 	if (notDeleted) {
	// 		this.workspace.decreaseMemberCount();
	// 		this.softDelete();
	// 	}
	// }
	//
	// @Deprecated
	// public void restoreMembership() {
	// 	if (this.isDeleted()) {
	// 		this.workspace.increaseMemberCount();
	// 		this.restore();
	// 	}
	// }

	public void remove() {
		this.workspace.decreaseMemberCount();
		this.member.getWorkspaceMembers().remove(this);
		this.workspace.getWorkspaceMembers().remove(this);
	}

	public void addPosition(Position position) {
		validatePositionBelongsToWorkspace(position);
		// validateDuplicateAssignedPosition(position);

		WorkspaceMemberPosition.builder()
			.workspaceMember(this)
			.position(position)
			.build();
	}

	public void removePosition(Position position) {
		WorkspaceMemberPosition workspaceMemberPosition = workspaceMemberPositions.stream()
			.filter(wmp -> wmp.getPosition().equals(position))
			.findFirst()
			.orElseThrow(() -> new InvalidOperationException(
				String.format(
					"Position '%s' is not assigned to this workspace member. workspaceMemberId: %d, positionId: %d",
					position.getName(), id, position.getId())
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
			.orElseThrow(() -> new InvalidOperationException(
				String.format("Team '%s' is not assigned to this workspace member. workspaceMemberId: %d, teamId: %d",
					team.getName(), id, team.getId())
			));

		workspaceMemberTeams.remove(workspaceMemberTeam);
	}

	public void updateRole(WorkspaceRole role) {
		validateCannotUpdateToOwnerRole(role);
		this.role = role;
	}

	// TODO: increaseMyWorkspaceCount, decreaseMyWorkspaceCount 호출은 어디서 하는 것이 제일 좋을까?
	public void updateRoleToAdmin() {
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
		if (role.isLowerThan(WorkspaceRole.MEMBER)) {
			throw new ForbiddenOperationException(
				String.format("Must have a workspace role higher than VIEWER. Current role: %s", role)
			);
		}
	}

	public boolean roleIsHigherThan(WorkspaceRole role) {
		return this.role.isHigherThan(role);
	}

	public boolean roleIsLowerThan(WorkspaceRole role) {
		return this.role.isLowerThan(role);
	}

	private void validatePositionBelongsToWorkspace(Position position) {
		if (!position.getWorkspaceCode().equals(workspaceCode)) {
			throw new InvalidOperationException(String.format(
				"Position does not belong to this workspace. position workspace code: %s, current workspace code: %s",
				position.getWorkspaceCode(), workspaceCode));
		}
	}

	private void validateDuplicateAssignedTeam(Team team) {
		boolean isAlreadyAssigned = workspaceMemberTeams.stream()
			.anyMatch(wmt -> wmt.getTeam().equals(team));

		if (isAlreadyAssigned) {
			throw new InvalidOperationException(
				String.format(
					"Team '%s' is already assigned to this workspace member. workspace member id: %d, team id: %d",
					team.getName(), id, team.getId()));
		}
	}

	private void validateTeamBelongsToWorkspace(Team team) {
		boolean teamWorkspaceCodeNotMatch = !team.getWorkspaceCode().equals(workspaceCode);

		if (teamWorkspaceCodeNotMatch) {
			throw new InvalidOperationException(
				String.format(
					"Team does not belong to this workspace. team workspace code: %s, current workspace code: %s",
					team.getWorkspaceCode(), workspace));
		}
	}

	private void validateCannotUpdateToOwnerRole(WorkspaceRole newRole) {
		if (newRole == WorkspaceRole.OWNER) {
			throw new InvalidOperationException(
				"Cannot directly change to OWNER role. Use ownership transfer instead.");
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
}
