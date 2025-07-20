package com.tissue.api.issue.domain.newmodel;

import java.util.ArrayList;
import java.util.List;

import com.tissue.api.common.entity.BaseEntity;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

// TODO: Have I set the UniqueConstraint properly?
//  A WorkflowDefinition must be unique for each Workspace by label.
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

	// TODO: Should I also add a workspaceCode field for usability?
	@ManyToOne(fetch = FetchType.LAZY)
	private Workspace workspace;

	@Column(nullable = false)
	private String key; // ex: DEFAULT_AGILE_WORKFLOW, CUSTOM_WORKFLOW_1...

	@Column(nullable = false)
	private String label; // UI label

	// TODO: Is using a bi-directional relationship good for WorkflowDefinition <-> WorkflowStep, WorkflowTransition?
	//  Or should I change these to a uni-directional relation?
	@OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<WorkflowStep> steps = new ArrayList<>();

	@OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<WorkflowTransition> transitions = new ArrayList<>();

	@ManyToOne(fetch = FetchType.LAZY)
	private WorkflowStep initialStep;

	@Builder
	public WorkflowDefinition(Workspace workspace, String key, String label) {
		this.workspace = workspace;
		this.key = key;
		this.label = label;
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

	public void updateKey(String key) {
		this.key = key;
	}

	public void updateLabel(String label) {
		this.label = label;
	}

	public void updateInitialStep(WorkflowStep newInitialStep) {
		// TODO: I'll probably validate the newInitialStep at service anyway,
		//  Is defensive programming needed?
		// if (!steps.contains(newInitialStep)) {
		// 	throw new InvalidOperationException("The step must be part of this workflow.");
		// }

		for (WorkflowStep step : steps) {
			step.setInitial(false);
		}

		newInitialStep.setInitial(true);
		this.initialStep = newInitialStep;
	}

	public List<WorkflowStep> getFinalSteps() {
		return steps.stream()
			.filter(WorkflowStep::isFinal)
			.toList();
	}
}
