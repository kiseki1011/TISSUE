package com.tissue.api.issue.domain.newmodel;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.issue.domain.util.KeyGenerator;
import com.tissue.api.workspace.domain.model.Workspace;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PostPersist;
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
	@UniqueConstraint(columnNames = {"workspace_id", "label"})
})
@EqualsAndHashCode(of = {"workspace", "label"}, callSuper = false)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkflowDefinition extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	private Workspace workspace;

	@Column(nullable = false)
	private String workspaceCode;

	@Column(nullable = false)
	private String key;

	@Column(nullable = false)
	private String label;

	@OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<WorkflowStep> steps = new HashSet<>();

	@OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<WorkflowTransition> transitions = new HashSet<>();

	@ManyToOne(fetch = FetchType.LAZY)
	private WorkflowStep initialStep;

	private String description;

	@PostPersist
	private void assignKey() {
		if (key == null && id != null) {
			key = KeyGenerator.generateWorkflowKey(id);
		}
	}

	@Builder
	public WorkflowDefinition(
		Workspace workspace,
		String key,
		String label,
		String description
	) {
		this.workspace = workspace;
		this.workspaceCode = workspace.getCode();
		this.key = key;
		this.label = label;
		this.description = description;
	}

	public void addStep(WorkflowStep step) {
		steps.add(step);
		step.setWorkflow(this);

		if (step.isInitialStep()) {
			updateInitialStep(step);
		}
	}

	public void addTransition(WorkflowTransition transition) {
		transitions.add(transition);
		transition.setWorkflow(this);
	}

	public void setKey(String key) {
		this.key = key;
	}

	public void updateLabel(String label) {
		this.label = label;
	}

	public void updateInitialStep(WorkflowStep newInitialStep) {
		if (!steps.contains(newInitialStep)) {
			throw new InvalidOperationException("The step must be part of this workflow.");
		}

		for (WorkflowStep step : steps) {
			step.setInitial(false);
		}

		newInitialStep.setInitial(true);
		this.initialStep = newInitialStep;
	}

	public void updateDescription(String description) {
		this.description = description;
	}

	public List<WorkflowStep> getFinalSteps() {
		return steps.stream()
			.filter(WorkflowStep::isFinal)
			.toList();
	}
}
