package com.tissue.api.issue.workflow.domain.model;

import static com.tissue.api.common.util.DomainPreconditions.*;

import org.hibernate.annotations.SQLRestriction;
import org.springframework.lang.Nullable;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.common.enums.ColorType;
import com.tissue.api.issue.base.domain.model.vo.Label;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

@Entity
@SQLRestriction("archived = false")
@Getter
@ToString(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkflowStatus extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@ToString.Include
	private Long id;

	@Version
	@ToString.Include
	private Long version;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "workflow_id")
	private Workflow workflow;

	@Embedded
	@ToString.Include
	private Label label;

	@Column(nullable = false, length = 255)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ColorType color;

	@Column(nullable = false)
	private boolean initial;

	@Column(nullable = false)
	private boolean terminal;

	static WorkflowStatus of(
		@NonNull Label label,
		@Nullable String description,
		@NonNull ColorType color,
		boolean initial,
		boolean terminal
	) {
		WorkflowStatus ws = new WorkflowStatus();
		ws.label = label;
		ws.description = nullToEmpty(description);
		ws.color = color;
		ws.initial = initial;
		ws.terminal = terminal;

		return ws;
	}

	void attachToWorkflow(@NonNull Workflow workflow) {
		this.workflow = workflow;
	}

	void updateLabel(@NonNull Label label) {
		this.label = label;
	}

	public void updateDescription(@Nullable String description) {
		this.description = nullToEmpty(description);
	}

	public void updateColor(@NonNull ColorType color) {
		this.color = color;
	}

	void markInitial() {
		this.initial = true;
	}

	void unmarkInitial() {
		this.initial = false;
	}

	void markTerminal() {
		this.terminal = true;
	}

	void unmarkTerminal() {
		this.terminal = false;
	}

	public void softDelete() {
		archive();
	}
}
