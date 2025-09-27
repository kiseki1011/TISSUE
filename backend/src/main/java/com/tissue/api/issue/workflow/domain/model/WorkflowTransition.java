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

	@Column(nullable = false)
	@ToString.Include
	private String label;

	@Column(nullable = false)
	private String description;

	// TODO: Should I change isMainFlow -> aMainFlow or mainFlow?
	@Column(nullable = false)
	private boolean isMainFlow;

	@ManyToOne(fetch = FetchType.LAZY)
	private WorkflowStatus sourceStatus;

	@ManyToOne(fetch = FetchType.LAZY)
	private WorkflowStatus targetStatus;

	@Builder
	public WorkflowTransition(
		Workflow workflow,
		Boolean isMainFlow,
		WorkflowStatus sourceStatus,
		WorkflowStatus targetStatus,
		String label,
		String description
	) {
		this.workflow = workflow;
		this.isMainFlow = isMainFlow;
		this.sourceStatus = sourceStatus;
		this.targetStatus = targetStatus;
		this.label = label;
		this.description = description;
	}

	public static WorkflowTransition create(
		@NonNull Workflow workflow,
		@NonNull String label,
		@Nullable String description,
		@NonNull Boolean isMainFlow,
		@NonNull WorkflowStatus sourceStatus,
		@NonNull WorkflowStatus targetStatus
	) {
		WorkflowTransition wt = new WorkflowTransition();
		wt.workflow = workflow;
		wt.label = label;
		wt.description = nullToEmpty(description);
		wt.isMainFlow = isMainFlow;
		wt.sourceStatus = sourceStatus;
		wt.targetStatus = targetStatus;

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

	public void updateSourceStatus(@NonNull WorkflowStatus sourceStatus) {
		this.sourceStatus = sourceStatus;
	}

	public void updateTargetStatus(@NonNull WorkflowStatus targetStatus) {
		this.targetStatus = targetStatus;
	}

	public void updateLabel(@NonNull String label) {
		this.label = label;
	}

	public void updateDescription(@Nullable String description) {
		this.description = description;
	}
}
