package com.tissue.api.workspace.domain;

import java.util.HashSet;
import java.util.Set;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.member.domain.model.Member;
import com.tissue.api.position.domain.model.Position;
import com.tissue.api.team.domain.model.Team;
import com.tissue.api.workspace.domain.enums.WorkspaceRole;

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
import lombok.Getter;
import lombok.NoArgsConstructor;

// TODO: Soft-delete 사용
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkspaceMember extends BaseEntity {

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

	// TODO: memberId도 편의 필드로 둘까? (workspaceKey와 마찬가지로 불변임)

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

	// TODO: bio? 자기소개? 추가 고려
	// private String bio;

	public static WorkspaceMember create(
		Member member,
		Workspace workspace,
		WorkspaceRole role
	) {
		WorkspaceMember workspaceMember = new WorkspaceMember();
		workspaceMember.workspace = workspace;
		workspaceMember.workspaceKey = workspace.getKey();
		workspaceMember.member = member;
		workspaceMember.email = member.getEmail();
		// TODO: 지금은 기본 displayName으로 member.getUsername()를 사용하지만, Member의 name을 필수 필드로 변경하고
		//  기본적으로 member.getName()을 사용하도록 설계 변경 고려
		workspaceMember.displayName = member.getUsername();
		workspaceMember.role = role;

		return workspaceMember;
	}

	public Long getMemberId() {
		return member.getId();
	}

	// TODO: WorkspaceMember 제거는 hard-delete vs soft-delete 중 뭘 사용하는게 좋을까?
	//  - WorkspaceMember를 Workspace에서 제거하는 경우 기존 해당 WorkspaceMember와 관련이 있는
	//  리소스(Issue, Sprint, Comment, etc...)에 대한 처리를 어떻게 해야할지 고민이 됨.
	//  - 만약 soft-delete이 권장된다면, 해당 soft-delete된 WorkspaceMember는 그대로 표시 가능한가?
	//  archived=true이므로, UI에서는 회색이나 반투명 회색으로 표기하는 형태로 가는게 좋을까?
	public void validateCanLeaveWorkspace() {
		if (this.role == WorkspaceRole.OWNER) {
			// TODO: OwnerCannotLeaveWorkspaceException
			throw new RuntimeException("Cannot leave workspace if workspace role is OWNER.");
		}
	}

	public void changeRoleTo(WorkspaceRole newRole) {
		if (role == newRole) {
			return;
		}
		if (newRole == WorkspaceRole.OWNER) {
			// TODO: DirectOwnerChangeNotAllowedException
			throw new RuntimeException("Cannot directly change to OWNER role. Use ownership transfer.");
		}
		this.role = newRole;
	}

	public void changeRoleToOwner() {
		this.role = WorkspaceRole.OWNER;
	}

	public void updateDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public boolean isOwner() {
		return this.role == WorkspaceRole.OWNER;
	}

	public boolean roleIsLowerThan(WorkspaceRole role) {
		return this.role.isLowerThan(role);
	}

	public boolean roleIsEqualOrHigherThan(WorkspaceRole role) {
		return this.role.isEqualOrHigherThan(role);
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
