package com.tissue.team.domain;

import java.util.ArrayList;
import java.util.List;

import org.springframework.lang.Nullable;

import com.tissue.common.entity.BaseEntity;
import com.tissue.common.enums.ColorType;
import com.tissue.common.vo.Name;
import com.tissue.workspace.domain.Workspace;
import com.tissue.workspace.domain.WorkspaceMemberTeam;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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
import lombok.NonNull;

@Entity
@Getter
@Table(uniqueConstraints = {
	@UniqueConstraint(
		name = "uk_workspace_team_name",
		columnNames = {"workspace_id", "team_name_norm"}
	)
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Team extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "team_id")
	private Long id;

	@Embedded
	@AttributeOverrides({
		@AttributeOverride(name = "value", column = @Column(name = "team_name", nullable = false, length = 64)),
		@AttributeOverride(name = "normalized", column = @Column(name = "team_name_norm", nullable = false, length = 64))
	})
	private Name name;

	@Column(name = "description")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(name = "color", nullable = false)
	private ColorType color;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "workspace_id", nullable = false)
	private Workspace workspace;

	@Column(name = "workspace_key", nullable = false)
	private String workspaceKey;

	@OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<WorkspaceMemberTeam> workspaceMemberTeams = new ArrayList<>();

	@Builder
	public Team(
		@NonNull Workspace workspace,
		@NonNull String name,
		@Nullable String description,
		@NonNull ColorType color
	) {
		this.workspace = workspace;
		this.workspaceKey = workspace.getKey();
		this.name = Name.of(name);
		this.description = description;
		this.color = color;
	}

	public void updateName(@NonNull String name) {
		this.name = Name.of(name);
	}

	public void updateDescription(@Nullable String description) {
		this.description = description;
	}

	public void updateColor(@NonNull ColorType color) {
		this.color = color;
	}

	public String getDisplayName() {
		return name.getDisplay();
	}
}
