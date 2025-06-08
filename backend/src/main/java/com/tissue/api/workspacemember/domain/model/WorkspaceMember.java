package com.tissue.api.workspacemember.domain.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
	private String displayName;

	@Column(nullable = false)
	private String email;

	// TODO: 캐싱은 추후에 고려
	//  (클라이언트에서 displayName, username을 한번에 조회하고 조합해서 사용하는 방식 고려
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
		this.workspaceCode = workspace.getCode();
	}

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

		return workspaceMember;
	}

	// TODO: increaseMyWorkspaceCount, decreaseMyWorkspaceCount 호출은 어디서 하는 것이 제일 좋을까?
	public static WorkspaceMember addOwnerWorkspaceMember(
		Member member,
		Workspace workspace
	) {
		member.increaseMyWorkspaceCount();
		workspace.increaseMemberCount();
		return createWorkspaceMember(member, workspace, WorkspaceRole.OWNER);
	}

	public static WorkspaceMember addWorkspaceMember(
		Member member,
		Workspace workspace
	) {
		workspace.increaseMemberCount();
		return createWorkspaceMember(member, workspace, WorkspaceRole.MEMBER);
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

	public void validateCanLeaveWorkspace() {
		if (this.role == WorkspaceRole.OWNER) {
			throw new InvalidOperationException("OWNER는 워크스페이스를 나갈 수 없습니다.");
		}
	}

	public void remove() {
		this.workspace.decreaseMemberCount();
		this.member.getWorkspaceMembers().remove(this);
		this.workspace.getWorkspaceMembers().remove(this);
	}

	public void addPosition(Position position) {
		validatePositionBelongsToWorkspace(position);

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
		validateUpdateToOwnerRole(role);
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

	public void updateDisplayName(String displayName) {
		this.displayName = displayName;
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
}
