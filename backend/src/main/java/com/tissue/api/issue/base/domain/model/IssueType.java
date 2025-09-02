package com.tissue.api.issue.base.domain.model;

import org.hibernate.annotations.SQLRestriction;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.common.enums.ColorType;
import com.tissue.api.common.util.TextNormalizer;
import com.tissue.api.global.key.KeyGenerator;
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
import jakarta.persistence.PostPersist;
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

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "workspace_id", nullable = false)
	private Workspace workspace;

	@Column(nullable = false, updatable = false, unique = true)
	private String key;

	@Column(nullable = false)
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

	@PostPersist
	private void assignKey() {
		if (key == null && id != null) {
			key = KeyGenerator.generateIssueTypeKey(id);
		}
	}

	@Builder
	public IssueType(
		Workspace workspace,
		String key,
		String label,
		String description,
		ColorType color,
		HierarchyLevel hierarchyLevel,
		Workflow workflow
	) {
		this.workspace = workspace;
		this.key = key;
		// TODO: use TextPreconditions, IssueTypeRules for non-null validation
		this.label = TextNormalizer.normalizeText(label);
		this.description = TextNormalizer.stripToEmpty(description);
		this.color = color != null ? color : ColorType.getRandomColor();
		this.hierarchyLevel = hierarchyLevel;
		this.workflow = workflow;
		this.systemType = false;
	}

	public String getWorkspaceCode() {
		return workspace.getKey();
	}

	public void updateMetaData(String label, String description, ColorType color) {
		updateLabel(label);
		updateDescription(description);
		updateColor(color);
	}

	public void updateLabel(String label) {
		// TODO: TextPreconditions.requireNonNull(label);
		this.label = TextNormalizer.normalizeText(label);
	}

	public void updateDescription(String description) {
		this.description = TextNormalizer.stripToEmpty(description);
	}

	public void updateColor(ColorType color) {
		// TODO: requireNonNull(color);
		this.color = color;
	}

	public void updateHierarchyLevel(HierarchyLevel hierarchyLevel) {
		// TODO: requireNonNull(hierarchyLevel);
		this.hierarchyLevel = hierarchyLevel;
	}

	public void setWorkflow(Workflow workflow) {
		// TODO: requireNonNull(workflow);
		this.workflow = workflow;
	}

	public void setAsSystemType() {
		this.systemType = true;
	}
}

