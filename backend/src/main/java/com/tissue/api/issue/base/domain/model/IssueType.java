package com.tissue.api.issue.base.domain.model;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.common.enums.ColorType;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

// TODO: Am I setting the @UniqueConstraint right?
@Entity
@Getter
@Table(uniqueConstraints = {
	@UniqueConstraint(columnNames = {"workspace_id", "label"})
})
@EqualsAndHashCode(of = {"workspace", "label"}, callSuper = false)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssueType extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	private Workspace workspace;

	@Column(nullable = false)
	private String key;

	@Column(nullable = false)
	private String label;

	// TODO: consider adding description field?
	// private String description;

	// private String icon;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ColorType color;

	@Column(nullable = false)
	private boolean systemType;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private HierarchyLevel hierarchyLevel;

	@ManyToOne(fetch = FetchType.LAZY)
	private Workflow workflow;

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
		ColorType color,
		HierarchyLevel hierarchyLevel,
		Workflow workflow
	) {
		this.workspace = workspace;
		this.key = key;
		this.label = label;
		this.color = color != null ? color : ColorType.getRandomColor();
		this.hierarchyLevel = hierarchyLevel;
		this.workflow = workflow;
		this.systemType = false;
	}

	public String getWorkspaceCode() {
		return workspace.getKey();
	}

	public void updateLabel(String label) {
		this.label = label;
	}

	public void updateColor(ColorType color) {
		this.color = color;
	}

	public void updateHierarchyLevel(HierarchyLevel hierarchyLevel) {
		this.hierarchyLevel = hierarchyLevel;
	}

	public void setWorkflow(Workflow workflow) {
		this.workflow = workflow;
	}

	public void setDefaultSystemType() {
		this.systemType = true;
	}
}

