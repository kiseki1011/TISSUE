package com.tissue.api.issuetype.domain;

import static com.tissue.api.common.util.DomainPreconditions.*;

import org.hibernate.annotations.SQLRestriction;
import org.springframework.lang.Nullable;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.common.enums.ColorType;
import com.tissue.api.issue.domain.enums.HierarchyLevel;
import com.tissue.api.issue.domain.model.vo.Label;
import com.tissue.api.workflow.domain.model.Workflow;
import com.tissue.api.workspace.domain.model.Workspace;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

@Entity
@SQLRestriction("archived = false")
@Table(
	indexes = @Index(name = "idx_issue_type_workspace_label", columnList = "workspace_id,label")
)
@Getter
@ToString(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssueType extends BaseEntity {

	// @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "issue_type_seq_gen")
	// @SequenceGenerator(name = "issue_type_seq_gen", sequenceName = "issue_type_seq", allocationSize = 50)
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@ToString.Include
	private Long id;

	@Version
	@ToString.Include
	private Long version;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "workspace_id", nullable = false)
	private Workspace workspace;

	@Embedded
	@ToString.Include
	private Label label;

	@Column(nullable = false, length = 255)
	private String description;

	// private String icon;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ColorType color;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private HierarchyLevel hierarchyLevel;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "workflow_id", nullable = false)
	private Workflow workflow;

	@Column(nullable = false)
	private boolean systemType;

	@Builder(access = AccessLevel.PRIVATE)
	private IssueType(
		Workspace workspace,
		Label label,
		String description,
		ColorType color,
		HierarchyLevel hierarchyLevel,
		Workflow workflow
	) {
		this.workspace = workspace;
		this.label = label;
		this.description = description;
		this.color = color;
		this.hierarchyLevel = hierarchyLevel;
		this.workflow = workflow;
		this.systemType = false;
	}

	public static IssueType create(
		@NonNull Workspace workspace,
		@NonNull Label label,
		@Nullable String description,
		@NonNull ColorType color,
		@NonNull HierarchyLevel hierarchyLevel,
		@NonNull Workflow workflow
	) {
		return IssueType.builder()
			.workspace(workspace)
			.label(label)
			.description(nullToEmpty(description))
			.color(color)
			.hierarchyLevel(hierarchyLevel)
			.workflow(workflow)
			.build();
	}

	public String getWorkspaceKey() {
		return workspace.getKey();
	}

	public void rename(@NonNull Label label) {
		this.label = label;
	}

	public void updateDescription(@Nullable String description) {
		this.description = nullToEmpty(description);
	}

	public void updateColor(@NonNull ColorType color) {
		this.color = color;
	}

	public void updateHierarchyLevel(@NonNull HierarchyLevel hierarchyLevel) {
		this.hierarchyLevel = hierarchyLevel;
	}

	public void setWorkflow(@NonNull Workflow workflow) {
		this.workflow = workflow;
	}

	public void setAsSystemType() {
		this.systemType = true;
	}

	public void softDelete() {
		archive();
	}
}

