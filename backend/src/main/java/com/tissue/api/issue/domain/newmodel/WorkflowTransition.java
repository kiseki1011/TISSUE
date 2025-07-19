package com.tissue.api.issue.domain.newmodel;

import jakarta.persistence.Column;
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

// TODO: Have I set the UniqueConstraint properly?
//  A WorkflowTransition must be unique for each WorkflowDefinition by label.
@Entity
@Getter
@Table(uniqueConstraints = {
	@UniqueConstraint(columnNames = {"workflow_id", "label"})
})
@EqualsAndHashCode(of = {"workflow", "label"}, callSuper = false) // see comment on key
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkflowTransition {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "workflow_id")
	private WorkflowDefinition workflow;

	// TODO: shouldn't it be OneToOne for fromStep and toStep?
	@ManyToOne(fetch = FetchType.LAZY)
	private WorkflowStep fromStep;

	@ManyToOne(fetch = FetchType.LAZY)
	private WorkflowStep toStep;

	// TODO: Should this field be unique globally? Or only by workflow?
	@Column(nullable = false)
	private String key; // SSM trigger event name for transition, ex: "START_PROGRESS", "MARK_DONE"

	// TODO: Consider using a util that transforms the UI label to name
	//  - ex: start progress -> START_PROGRESS
	@Column(nullable = false)
	private String label; // UI label

	// private String guardKey;   // ex: "REQUIRES_APPROVAL", "NOT_BLOCKED"

	@Builder
	public WorkflowTransition(
		WorkflowDefinition workflow,
		WorkflowStep fromStep,
		WorkflowStep toStep,
		String key,
		String label
	) {
		this.workflow = workflow;
		this.fromStep = fromStep;
		this.toStep = toStep;
		this.key = key;
		this.label = label;
	}

	public void setWorkflow(WorkflowDefinition workflow) {
		this.workflow = workflow;
	}

	public void updateFromStep(WorkflowStep fromStep) {
		this.fromStep = fromStep;
	}

	public void updateToStep(WorkflowStep toStep) {
		this.toStep = toStep;
	}

	public void updateKey(String key) {
		this.key = key;
	}

	public void updateLabel(String label) {
		this.label = label;
	}
}
