package com.tissue.api.position.domain;

import java.util.ArrayList;
import java.util.List;

import com.tissue.api.common.ColorType;
import com.tissue.api.common.entity.WorkspaceContextBaseEntity;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspacemember.domain.WorkspaceMember;

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
		name = "UK_WORKSPACE_POSITION_NAME",
		columnNames = {"workspace_code", "name"}
	)
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Position extends WorkspaceContextBaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "POSITION_ID")
	private Long id;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ColorType color;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "WORKSPACE_ID", nullable = false)
	private Workspace workspace;

	@Column(name = "WORKSPACE_CODE", nullable = false)
	private String workspaceCode;

	@OneToMany(mappedBy = "position")
	private List<WorkspaceMember> workspaceMembers = new ArrayList<>();

	@Builder
	public Position(
		String name,
		String description,
		Workspace workspace,
		ColorType color
	) {
		this.name = name;
		this.description = description;
		this.workspace = workspace;
		this.workspaceCode = workspace.getCode();
		this.color = color;
		workspace.getPositions().add(this);
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
}
