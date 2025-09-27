package com.tissue.api.issue.workflow.domain.model;

import static com.tissue.api.common.util.DomainPreconditions.*;

import org.springframework.lang.Nullable;

import com.tissue.api.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

@Entity
@Getter
@ToString(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkflowTransition extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@ToString.Include
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "workflow_id")
	private Workflow workflow;

	// TODO: Should I change isMainFlow -> aMainFlow or mainFlow?
	@Column(nullable = false)
	private boolean isMainFlow;

	@ManyToOne(fetch = FetchType.LAZY)
	private WorkflowStatus sourceStep;

	@ManyToOne(fetch = FetchType.LAZY)
	private WorkflowStatus targetStep;

	@Column(nullable = false)
	@ToString.Include
	private String label;

	@Column(nullable = false)
	private String description;

	@Builder
	public WorkflowTransition(
		Workflow workflow,
		Boolean isMainFlow,
		WorkflowStatus sourceStep,
		WorkflowStatus targetStep,
		String label,
		String description
	) {
		this.workflow = workflow;
		this.isMainFlow = isMainFlow;
		this.sourceStep = sourceStep;
		this.targetStep = targetStep;
		this.label = label;
		this.description = description;
	}

	public static WorkflowTransition create(
		@NonNull Workflow workflow,
		@NonNull Boolean isMainFlow,
		@NonNull WorkflowStatus sourceStep,
		@NonNull WorkflowStatus targetStep,
		@NonNull String label,
		@Nullable String description
	) {
		WorkflowTransition wt = new WorkflowTransition();
		wt.workflow = workflow;
		wt.isMainFlow = isMainFlow;
		wt.sourceStep = sourceStep;
		wt.targetStep = targetStep;
		wt.label = label;
		wt.description = nullToEmpty(description);

		return wt;
	}

	public void setWorkflow(@NonNull Workflow workflow) {
		this.workflow = workflow;
	}

	public void updateIsMainFlow(@NonNull Boolean isMainFlow) {
		// TODO: Should I add validation logic so the main flow will maintain a single straight flow?
		//  If I should, where should i perform the validation? In this Entity? or at the application service layer?
		this.isMainFlow = isMainFlow;
	}

	public void updateSourceStep(@NonNull WorkflowStatus sourceStep) {
		this.sourceStep = sourceStep;
	}

	public void updateTargetStep(@NonNull WorkflowStatus targetStep) {
		this.targetStep = targetStep;
	}

	public void updateLabel(@NonNull String label) {
		this.label = label;
	}

	public void updateDescription(@Nullable String description) {
		this.description = description;
	}
}
