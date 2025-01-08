package com.tissue.api.team.domain;

import com.tissue.api.workspacemember.domain.WorkspaceMember;

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
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	uniqueConstraints = {
		@UniqueConstraint(
			name = "UK_WORKSPACE_MEMBER_TEAM",
			columnNames = {"WORKSPACE_MEMBER_ID", "TEAM_ID"})
	}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkspaceMemberTeam {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "WORKSPACE_MEMBER_ID", nullable = false)
	private WorkspaceMember workspaceMember;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "TEAM_ID", nullable = false)
	private Team team;

	@Builder
	public WorkspaceMemberTeam(WorkspaceMember workspaceMember, Team team) {
		this.workspaceMember = workspaceMember;
		this.team = team;

		workspaceMember.getWorkspaceMemberTeams().add(this);
		team.getWorkspaceMemberTeams().add(this);
	}
}
