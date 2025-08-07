package com.tissue.api.team.domain.model;

import java.util.ArrayList;
import java.util.List;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.common.enums.ColorType;
import com.tissue.api.workspace.domain.model.Workspace;
import com.tissue.api.workspacemember.domain.model.WorkspaceMemberTeam;

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
@Getter
@Table(uniqueConstraints = {
	@UniqueConstraint(
		name = "uk_workspace_team_name",
		columnNames = {"workspace_id", "name"}
	)
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Team extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "team_id")
	private Long id;

	@Column(name = "name", nullable = false)
	private String name;

	// TODO: should i allow null for description
	@Column(name = "description")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(name = "color", nullable = false)
	private ColorType color;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "workspace_id", nullable = false)
	private Workspace workspace;

	// TODO: Should i use Set, and override EqualsAndHashCode as {"workspaceMember", "team"}?
	@OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<WorkspaceMemberTeam> workspaceMemberTeams = new ArrayList<>();

	@Builder
	public Team(
		String name,
		String description,
		Workspace workspace,
		ColorType color
	) {
		this.name = name;
		this.description = description;
		this.workspace = workspace;
		this.color = color;
	}

	public void updateName(String name) {
		this.name = name;
	}

	public void updateDescription(String description) {
		this.description = description;
	}

	public void updateColor(ColorType color) {
		this.color = color;
	}

	public String getWorkspaceKey() {
		return workspace.getKey();
	}
}
