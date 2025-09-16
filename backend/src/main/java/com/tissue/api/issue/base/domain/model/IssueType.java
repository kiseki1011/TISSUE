package com.tissue.api.issue.base.domain.model;

import java.util.Objects;

import org.hibernate.annotations.SQLRestriction;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.common.enums.ColorType;
import com.tissue.api.common.util.DomainPreconditions;
import com.tissue.api.issue.base.domain.enums.HierarchyLevel;
import com.tissue.api.issue.workflow.domain.model.Workflow;
import com.tissue.api.workspace.domain.model.Workspace;

import jakarta.persistence.Column;
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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@SQLRestriction("archived = false")
@Table(
	// uniqueConstraints = {@UniqueConstraint(columnNames = {"workspace_id", "label"})},
	indexes = @Index(name = "idx_issue_type_workspace_label", columnList = "workspace_id,label")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssueType extends BaseEntity {

	// @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "issue_type_seq_gen")
	// @SequenceGenerator(name = "issue_type_seq_gen", sequenceName = "issue_type_seq", allocationSize = 50)
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "workspace_id", nullable = false)
	private Workspace workspace;

	@Column(nullable = false, length = 32)
	private String label;

	@Column(nullable = false)
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

	@Builder
	private IssueType(
		Workspace workspace,
		String label,
		String description,
		ColorType color,
		HierarchyLevel hierarchyLevel,
		Workflow workflow
	) {
		this.workspace = Objects.requireNonNull(workspace);
		this.label = DomainPreconditions.requireNotBlank(label, "label");
		this.description = DomainPreconditions.nullToEmpty(description);
		this.color = DomainPreconditions.requireNotNull(color, "color");
		this.hierarchyLevel = DomainPreconditions.requireNotNull(hierarchyLevel, "hierarchyLevel");
		this.workflow = DomainPreconditions.requireNotNull(workflow, "workflow");
		this.systemType = false;
	}

	public static IssueType create(Workspace workspace, String label, String description, ColorType color,
		HierarchyLevel hierarchyLevel, Workflow workflow
	) {
		return IssueType.builder()
			.workspace(workspace)
			.label(label)
			.description(description)
			.color(color)
			.hierarchyLevel(hierarchyLevel)
			.workflow(workflow)
			.build();
	}

	public String getWorkspaceCode() {
		return workspace.getKey();
	}

	public void rename(String label) {
		this.label = DomainPreconditions.requireNotBlank(label, "label");
	}

	public void updateDescription(String description) {
		this.description = DomainPreconditions.nullToEmpty(description);
	}

	public void updateColor(ColorType color) {
		this.color = DomainPreconditions.requireNotNull(color, "color");
	}

	public void updateHierarchyLevel(HierarchyLevel hierarchyLevel) {
		this.hierarchyLevel = DomainPreconditions.requireNotNull(hierarchyLevel, "hierarchyLevel");
	}

	public void setWorkflow(Workflow workflow) {
		this.workflow = DomainPreconditions.requireNotNull(workflow, "workflow");
	}

	public void setAsSystemType() {
		this.systemType = true;
	}

	public void softDelete() {
		archive();
	}
}

