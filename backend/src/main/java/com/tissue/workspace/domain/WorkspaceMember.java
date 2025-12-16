package com.tissue.workspace.domain;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.SQLRestriction;

import com.tissue.common.entity.BaseEntity;
import com.tissue.member.domain.Member;
import com.tissue.position.domain.model.Position;
import com.tissue.team.domain.model.Team;
import com.tissue.workspace.domain.enums.WorkspaceRole;

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

@Entity
@SQLRestriction("softDeleted = false")
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

	public String getUsername() {
		return member.getUsername();
	}

	public String getEmail() {
		return member.getEmail();
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

	// TODO: 함부러 호출하지 않도록 주석 필요
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
		WorkspaceMemberPosition.create(this, position);
	}

	public void removePosition(Position position) {
		WorkspaceMemberPosition wmp = this.workspaceMemberPositions.stream()
			.filter(w -> w.getPosition().equals(position))
			.findFirst()
			.orElse(null);

		if (wmp != null) {
			this.workspaceMemberPositions.remove(wmp);
			position.getWorkspaceMemberPositions().remove(wmp);
		}
	}

	public void addTeam(Team team) {
		WorkspaceMemberTeam.create(this, team);
	}

	public void removeTeam(Team team) {
		WorkspaceMemberTeam wmp = this.workspaceMemberTeams.stream()
			.filter(w -> w.getTeam().equals(team))
			.findFirst()
			.orElse(null);

		if (wmp != null) {
			this.workspaceMemberTeams.remove(wmp);
			team.getWorkspaceMemberTeams().remove(wmp);
		}
	}
}
