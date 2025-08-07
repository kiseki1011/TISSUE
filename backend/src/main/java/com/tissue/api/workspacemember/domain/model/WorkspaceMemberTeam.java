package com.tissue.api.workspacemember.domain.model;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.team.domain.model.Team;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_workspace_member_team",
			columnNames = {"workspace_member_id", "team_id"})
	}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(of = {"workspaceMember", "team"}, callSuper = false)
public class WorkspaceMemberTeam extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "workspace_member_id", nullable = false)
	private WorkspaceMember workspaceMember;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "team_id", nullable = false)
	private Team team;

	@Builder
	public WorkspaceMemberTeam(WorkspaceMember workspaceMember, Team team) {
		this.workspaceMember = workspaceMember;
		this.team = team;

		workspaceMember.getWorkspaceMemberTeams().add(this);
		team.getWorkspaceMemberTeams().add(this);
	}
}
