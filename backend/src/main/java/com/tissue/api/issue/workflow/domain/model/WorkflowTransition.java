package com.tissue.api.issue.workflow.domain.model;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.global.key.KeyGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostPersist;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// TODO: Am I setting the @UniqueConstraint right?
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkflowTransition extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// TODO: Should I use uni or bi directional relation with WorkflowDefinition?
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "workflow_id")
	private Workflow workflow;

	@Column(nullable = false)
	private boolean isMainFlow;

	@ManyToOne(fetch = FetchType.LAZY)
	private WorkflowStatus sourceStep;

	@ManyToOne(fetch = FetchType.LAZY)
	private WorkflowStatus targetStep;

	@Column(name = "wf_transition_key", nullable = false)
	private String key;

	@Column(nullable = false)
	private String label;

	private String description;

	@PostPersist
	private void assignKey() {
		if (key == null && id != null) {
			key = KeyGenerator.generateTransitionKey(id);
		}
	}

	@Builder
	public WorkflowTransition(
		Workflow workflow,
		Boolean isMainFlow,
		WorkflowStatus sourceStep,
		WorkflowStatus targetStep,
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
		this.description = description != null ? description : "";
	}

	public void setWorkflow(Workflow workflow) {
		this.workflow = workflow;
	}

	public void updateIsMainFlow(boolean isMainFlow) {
		// TODO: Should I add validation logic so the main flow will maintain a single straight flow?
		//  If I should, where should i perform the validation? In this Entity? or at the application service layer?
		this.isMainFlow = isMainFlow;
	}

	public void updateSourceStep(WorkflowStatus sourceStep) {
		this.sourceStep = sourceStep;
	}

	public void updateTargetStep(WorkflowStatus targetStep) {
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
