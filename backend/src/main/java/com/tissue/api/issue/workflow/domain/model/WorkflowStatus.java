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
public class WorkflowStatus extends BaseEntity {

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

	private String description;

	// TODO: Change isInitial -> aInitial
	@Column(nullable = false)
	private boolean isInitial;

	// TODO: Change isFinal -> aFinal
	@Column(nullable = false)
	private boolean isFinal;

	// TODO: consider adding fields for color, icons, etc...

	@Builder
	public WorkflowStatus(
		Workflow workflow,
		String label,
		Boolean isInitial,
		Boolean isFinal,
		String description
	) {
		this.workflow = workflow;
		this.label = label;
		this.isInitial = isInitial;
		this.isFinal = isFinal;
		this.description = description;
	}

	public static WorkflowStatus create(
		@NonNull Workflow workflow,
		@NonNull String label,
		@Nullable String description,
		@NonNull Boolean isInitial,
		@NonNull Boolean isFinal
	) {
		WorkflowStatus ws = new WorkflowStatus();
		ws.workflow = workflow;
		ws.label = label;
		ws.description = nullToEmpty(description);
		ws.isInitial = isInitial;
		ws.isFinal = isFinal;

		return ws;
	}

	public void setWorkflow(@NonNull Workflow workflow) {
		this.workflow = workflow;
	}

	public void setInitial(@NonNull Boolean isInitial) {
		this.isInitial = isInitial;
	}

	public void setFinal(@NonNull Boolean isFinal) {
		this.isFinal = isFinal;
	}

	public void updateLabel(@NonNull String label) {
		this.label = label;
	}

	public void updateDescription(@Nullable String description) {
		this.description = description;
	}

	public boolean isInitialStatus() {
		return isInitial;
	}

	public boolean isFinalStatus() {
		return isFinal;
	}
}
