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
public class WorkflowStatus extends BaseEntity {

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
	private boolean initial;

	@Column(nullable = false)
	private boolean terminal;

	// TODO: consider adding fields for color, icons, etc...

	public static WorkflowStatus create(
		@NonNull Workflow workflow,
		@NonNull Label label,
		@Nullable String description,
		@NonNull Boolean initial,
		@NonNull Boolean terminal
	) {
		WorkflowStatus ws = new WorkflowStatus();
		ws.workflow = workflow;
		ws.label = label;
		ws.description = nullToEmpty(description);
		ws.initial = initial;
		ws.terminal = terminal;

		return ws;
	}

	public void setWorkflow(@NonNull Workflow workflow) {
		this.workflow = workflow;
	}

	public void setInitialFlag(boolean value) {
		this.initial = value;
	}

	public void setTerminalFlag(boolean value) {
		this.terminal = value;
	}

	public void updateDescription(@Nullable String description) {
		this.description = description;
	}

	public boolean isInitialStatus() {
		return initial;
	}

	public boolean isFinalStatus() {
		return terminal;
	}
}
