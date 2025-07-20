package com.tissue.api.issue.domain.newmodel;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.common.enums.ColorType;
import com.tissue.api.issue.domain.model.enums.HierarchyLevel;
import com.tissue.api.workspace.domain.model.Workspace;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

// TODO: Have I set the UniqueConstraint properly?
//  A IssueTypeDefinition must be unique for each Workspace by label.
@Entity
@Getter
@Table(uniqueConstraints = {
	@UniqueConstraint(columnNames = {"workspace_id", "label"})
})
@EqualsAndHashCode(of = {"workspace", "label"}, callSuper = false)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssueTypeDefinition extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	private Workspace workspace;

	@Column(nullable = false)
	private String key; // ex: "EPIC", "BUG", "CUSTOM_TYPE_1"

	@Column(nullable = false)
	private String label; // UI label

	// private String icon;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ColorType color;

	@Column(nullable = false)
	private boolean systemType; // true = built-in default types

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private HierarchyLevel hierarchyLevel;

	@ManyToOne(fetch = FetchType.LAZY)
	private WorkflowDefinition workflow;

	@Builder
	public IssueTypeDefinition(
		Workspace workspace,
		String key,
		String label,
		ColorType color,
		HierarchyLevel hierarchyLevel,
		WorkflowDefinition workflow
	) {
		this.workspace = workspace;
		this.key = key;
		this.label = label;
		this.color = color != null ? color : ColorType.getRandomColor();
		this.hierarchyLevel = hierarchyLevel;
		this.workflow = workflow;

		this.systemType = false;
	}

	public void updateKey(String key) {
		this.key = key;
	}

	public void updateLabel(String label) {
		this.label = label;
	}

	public void updateColor(ColorType color) {
		this.color = color;
	}

	public void setSystemType(boolean systemType) {
		this.systemType = systemType;
	}

	public void updateHierarchyLevel(HierarchyLevel hierarchyLevel) {
		this.hierarchyLevel = hierarchyLevel;
	}

	public void setWorkflow(WorkflowDefinition workflow) {
		this.workflow = workflow;
	}
}

