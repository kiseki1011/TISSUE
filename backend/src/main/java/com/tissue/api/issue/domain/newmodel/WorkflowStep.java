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
//  A WorkflowStep must be unique for each WorkflowDefinition by label.
@Entity
@Getter
@Table(uniqueConstraints = {
	@UniqueConstraint(columnNames = {"workflow_id", "label"})
})
@EqualsAndHashCode(of = {"workflow", "label"}, callSuper = false)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkflowStep {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "workflow_id")
	private WorkflowDefinition workflow;

	@Column(nullable = false)
	private String key; // used for SSM, ex: "TODO", "IN_PROGRESS"

	// TODO: consider using a util that transforms the UI label to name
	//  - ex: in progress -> IN_PROGRESS, in review -> IN_REVIEW
	@Column(nullable = false)
	private String label; // UI label

	// TODO: I understand why isFinal is needed, but is isInitial needed too?
	private boolean isInitial;
	private boolean isFinal;

	// TODO: consider adding fields for color, icons, etc...

	@Builder
	public WorkflowStep(
		WorkflowDefinition workflow,
		String key,
		String label,
		boolean isInitial,
		boolean isFinal
	) {
		this.workflow = workflow;
		this.key = key;
		this.label = label;
		// TODO: should I set isInitial, isFinal as false if value is null?
		this.isInitial = isInitial;
		this.isFinal = isFinal;
	}

	public void setWorkflow(WorkflowDefinition workflow) {
		this.workflow = workflow;
	}

	public void setIsInitial(boolean isInitial) {
		this.isInitial = isInitial;
	}

	public void setIsFinal(boolean isFinal) {
		this.isFinal = isFinal;
	}

	// public void updateKey(String key) {
	// 	this.key = key;
	// }

	public void updateLabel(String label) {
		this.label = label;
	}

	public boolean isInitialStep() {
		return isInitial;
	}

	public boolean isFinalStep() {
		return isFinal;
	}
}
