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

	@ManyToOne(fetch = FetchType.LAZY)
	private WorkflowStatus sourceStatus;

	@ManyToOne(fetch = FetchType.LAZY)
	private WorkflowStatus targetStatus;

	@Column(nullable = false)
	private boolean mainFlow;

	public static WorkflowTransition of(
		@NonNull Label label,
		@Nullable String description,
		@NonNull WorkflowStatus sourceStatus,
		@NonNull WorkflowStatus targetStatus,
		boolean mainFlow
	) {
		WorkflowTransition wt = new WorkflowTransition();
		wt.label = label;
		wt.description = nullToEmpty(description);
		wt.sourceStatus = sourceStatus;
		wt.targetStatus = targetStatus;
		wt.mainFlow = mainFlow;

		return wt;
	}

	void _attachToWorkflow(@NonNull Workflow workflow) {
		this.workflow = workflow;
	}

	void _includeInMainFlow() {
		this.mainFlow = true;
	}

	void _excludeFromMainFlow() {
		this.mainFlow = false;
	}

	void _rewireSource(@NonNull WorkflowStatus sourceStatus) {
		this.sourceStatus = sourceStatus;
	}

	void _rewireTarget(@NonNull WorkflowStatus targetStatus) {
		this.targetStatus = targetStatus;
	}

	void _updateLabel(@NonNull Label label) {
		this.label = label;
	}

	void _updateDescription(@Nullable String description) {
		this.description = nullToEmpty(description);
	}

	public void softDelete() {
		archive();
	}
}
