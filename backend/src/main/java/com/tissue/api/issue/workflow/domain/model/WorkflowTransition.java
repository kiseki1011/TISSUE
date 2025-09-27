package com.tissue.api.issue.workflow.domain.model;

import static com.tissue.api.common.util.DomainPreconditions.*;

import org.springframework.lang.Nullable;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.issue.base.domain.model.vo.Label;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
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

	@Embedded
	@ToString.Include
	private Label label;

	@Column(nullable = false, length = 255)
	private String description;

	@Column(nullable = false)
	private boolean mainFlow;

	@ManyToOne(fetch = FetchType.LAZY)
	private WorkflowStatus sourceStatus;

	@ManyToOne(fetch = FetchType.LAZY)
	private WorkflowStatus targetStatus;

	public static WorkflowTransition create(
		@NonNull Workflow workflow,
		@NonNull Label label,
		@Nullable String description,
		@NonNull Boolean mainFlow,
		@NonNull WorkflowStatus sourceStatus,
		@NonNull WorkflowStatus targetStatus
	) {
		WorkflowTransition wt = new WorkflowTransition();
		wt.workflow = workflow;
		wt.label = label;
		wt.description = nullToEmpty(description);
		wt.mainFlow = mainFlow;
		wt.sourceStatus = sourceStatus;
		wt.targetStatus = targetStatus;

		return wt;
	}

	public void setWorkflow(@NonNull Workflow workflow) {
		this.workflow = workflow;
	}

	public void includeInMainFlow() {
		this.mainFlow = true;
	}

	public void excludeFromMainFlow() {
		this.mainFlow = false;
	}

	public void updateSourceStatus(@NonNull WorkflowStatus sourceStatus) {
		this.sourceStatus = sourceStatus;
	}

	public void updateTargetStatus(@NonNull WorkflowStatus targetStatus) {
		this.targetStatus = targetStatus;
	}

	public void updateLabel(@NonNull Label label) {
		this.label = label;
	}

	public void updateDescription(@Nullable String description) {
		this.description = description;
	}
}
