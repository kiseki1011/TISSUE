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

@Entity
@Getter
@Table(uniqueConstraints = {
	@UniqueConstraint(columnNames = {"workflow_id", "source_step_id", "key"})
})
@EqualsAndHashCode(of = {"workflow", "key"}, callSuper = false)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkflowTransition {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "workflow_id")
	private WorkflowDefinition workflow;

	@Column(nullable = false)
	private boolean isMainFlow;

	@ManyToOne(fetch = FetchType.LAZY)
	private WorkflowStep sourceStep;

	@ManyToOne(fetch = FetchType.LAZY)
	private WorkflowStep targetStep;

	@Column(nullable = false)
	private String key;

	@Column(nullable = false)
	private String label;

	private String description;

	// private String guardKey;   // ex: "REQUIRES_APPROVAL", "NOT_BLOCKED"

	@Builder
	public WorkflowTransition(
		WorkflowDefinition workflow,
		Boolean isMainFlow,
		WorkflowStep sourceStep,
		WorkflowStep targetStep,
		String key,
		String label,
		String description
	) {
		this.isMainFlow = isMainFlow;
		this.workflow = workflow;
		this.sourceStep = sourceStep;
		this.targetStep = targetStep;
		this.key = key;
		this.label = label;
		this.description = description;
	}

	public void setWorkflow(WorkflowDefinition workflow) {
		this.workflow = workflow;
	}

	public void updateIsMainFlow(boolean isMainFlow) {
		// TODO: Should I add validation logic so the main flow will maintain a single straight flow?
		//  If I should, where should i perform the validation? In this Entity? or at the application service layer?
		this.isMainFlow = isMainFlow;
	}

	public void updateSourceStep(WorkflowStep sourceStep) {
		this.sourceStep = sourceStep;
	}

	public void updateTargetStep(WorkflowStep targetStep) {
		this.targetStep = targetStep;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public void updateLabel(String label) {
		this.label = label;
	}

	public void updateDescription(String description) {
		this.description = description;
	}
}
