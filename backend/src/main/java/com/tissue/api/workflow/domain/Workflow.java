package com.tissue.api.workflow.domain;

import static com.tissue.api.common.util.TextNormalizer.*;
import static com.tissue.api.issue.domain.enums.StateCategory.*;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.SQLRestriction;
import org.springframework.lang.Nullable;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.common.enums.ColorType;
import com.tissue.api.common.vo.Label;
import com.tissue.api.issue.domain.enums.StateCategory;
import com.tissue.api.project.domain.Project;
import com.tissue.api.workflow.domain.gaurd.GuardType;
import com.tissue.api.workflow.exception.DuplicateStateException;
import com.tissue.api.workflow.exception.DuplicateTransitionException;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Entity
@SQLRestriction("archived = false")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Workflow extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Version
	private Long version;

	@ManyToOne(fetch = FetchType.LAZY)
	private Project project;

	@Embedded
	private Label label;

	@Column(nullable = false, length = 255)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ColorType color;

	@OneToMany(mappedBy = "workflow", cascade = CascadeType.PERSIST)
	private List<WorkflowState> states = new ArrayList<>();

	@OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<WorkflowTransition> transitions = new ArrayList<>();

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "initial_state_id")
	private WorkflowState initialState;

	@Column(nullable = false)
	private boolean systemProvided;

	public static Workflow create(
		@NonNull Project project,
		@NonNull Label label,
		@Nullable String description,
		@NonNull ColorType color
	) {
		Workflow wf = new Workflow();
		wf.project = project;
		wf.label = label;
		wf.description = nullToEmpty(description);
		wf.color = color;
		wf.systemProvided = false;

		return wf;
	}

	public WorkflowState addState(
		@NonNull Label label,
		@Nullable String description,
		@NonNull ColorType color,
		@NonNull StateCategory stateCategory
	) {
		ensureUniqueStateLabel(label);

		// TODO: 그냥 initialState의 존재 여부를 검사하면 되는거 아님?
		if (stateCategory.isTodo()) {
			ensureNoExistingTodoState();
		}

		WorkflowState state = WorkflowState.of(label, description, color, stateCategory);
		state.attachToWorkflow(this);
		states.add(state);

		if (stateCategory.isTodo()) {
			this.initialState = state;
		}

		return state;
	}

	public WorkflowTransition addTransition(
		@NonNull Label label,
		@Nullable String description,
		@NonNull WorkflowState source,
		@NonNull WorkflowState target
	) {
		ensureUniqueTransitionLabelForSource(label, source);
		ensureNoDuplicateEdge(source, target);

		WorkflowTransition transition = WorkflowTransition.of(label, description, source, target);
		transition.attachToWorkflow(this);
		transitions.add(transition);

		return transition;
	}

	public List<WorkflowState> getActiveStates() {
		return states.stream()
			.filter(s -> !s.isArchived())
			.toList();
	}

	public List<WorkflowState> getStatesByCategory(StateCategory category) {
		return states.stream()
			.filter(s -> !s.isArchived() && s.getCategory() == category)
			.toList();
	}

	private void ensureNoExistingTodoState() {
		boolean hasTodo = !getStatesByCategory(TODO).isEmpty();
		if (hasTodo) {
			throw new RuntimeException("Workflow can have only one TODO state.");
		}
	}

	public void setInitialState(@NonNull WorkflowState state) {
		if (!states.contains(state)) {
			throw new IllegalArgumentException("State must belong to this workflow.");
		}
		if (state.getCategory().isNotTodo()) {
			throw new IllegalArgumentException("Initial state must be of category TODO.");
		}
		this.initialState = state;
	}

	public void setAsSystemProvided() {
		this.systemProvided = true;
	}

	public void rename(@NonNull Label label) {
		this.label = label;
	}

	public void updateDescription(@Nullable String description) {
		this.description = nullToEmpty(description);
	}

	public void updateColor(@Nullable ColorType color) {
		this.color = color;
	}

	public void changeInitialState(@NonNull WorkflowState newInitState) {
		for (WorkflowState s : states) {
			this.initialState.categorizeAs(IN_PROGRESS);
		}
		newInitState.categorizeAs(TODO);
		this.initialState = newInitState;
	}

	public void softDeleteState(@NonNull WorkflowState state) {
		if (state.getCategory().isTodo()) {
			// TODO: 예외 추가하기
			throw new RuntimeException("Cannot delete the TODO state. It is the initial state.");
		}
		state.softDelete();
		states.remove(state);
	}

	public void deleteTransition(@NonNull WorkflowTransition transition) {
		transitions.remove(transition);
	}

	public void renameState(@NonNull WorkflowState state, @NonNull Label newLabel) {
		if (state.getLabel().equals(newLabel)) {
			return;
		}
		ensureUniqueStateLabel(newLabel);
		state.updateLabel(newLabel);
	}

	public void renameTransition(@NonNull WorkflowTransition transition, @NonNull Label newLabel) {
		if (transition.getLabel().equals(newLabel)) {
			return;
		}
		ensureUniqueTransitionLabelForSource(newLabel, transition.getSourceState());
		transition.updateLabel(newLabel);
	}

	public void changeStateCategory(WorkflowState state, StateCategory newCategory) {
		if (state.getCategory() == newCategory) {
			return;
		}
		if (newCategory.isTodo()) {
			ensureNoExistingTodoState();
			this.initialState = state;
		}
		if (state.getCategory().isTodo()) {
			this.initialState = null;
		}

		state.categorizeAs(newCategory);
	}

	// public void categorizeStateAsTodo(@NonNull WorkflowState state) {
	// 	if (state.getCategory().isTodo()) {
	// 		return;
	// 	}
	// 	state.categorizeAs(TODO);
	// 	this.initialState = state;
	// }
	//
	// public void categorizeStateAsDone(@NonNull WorkflowState state) {
	// 	if (state.getCategory().isDone()) {
	// 		return;
	// 	}
	// 	if (state.getCategory().isTodo()) {
	// 		this.initialState = null;
	// 	}
	// 	state.categorizeAs(DONE);
	// }
	//
	// public void categorizeStateAsInProgress(@NonNull WorkflowState state) {
	// 	if (state.getCategory().isInProgress()) {
	// 		return;
	// 	}
	// 	if (state.getCategory().isTodo()) {
	// 		this.initialState = null;
	// 	}
	// 	state.categorizeAs(IN_PROGRESS);
	// }

	public void rewireTransitionSource(@NonNull WorkflowTransition transition, @NonNull WorkflowState newSource) {
		transition.rewireSource(newSource);
	}

	public void rewireTransitionTarget(@NonNull WorkflowTransition transition, @NonNull WorkflowState newTarget) {
		transition.rewireTarget(newTarget);
	}

	public void addTransitionGuard(
		@NonNull WorkflowTransition transition,
		@NonNull GuardType guardType,
		@Nullable String params,
		int order
	) {
		transition.addGuard(guardType, params, order);
	}

	public void clearGuardsForTransition(@NonNull WorkflowTransition transition) {
		transition.clearGuards();
	}

	private void ensureNoDuplicateEdge(WorkflowState source, WorkflowState target) {
		boolean dup = transitions.stream()
			.filter(t -> !t.isArchived())
			.anyMatch(x -> x.getSourceState().equals(source) && x.getTargetState().equals(target));
		if (dup) {
			throw new RuntimeException("Duplicate transition (source,target) is not allowed.");
		}
	}

	private void ensureUniqueStateLabel(Label newLabel) {
		boolean dup = states.stream()
			.filter(t -> !t.isArchived())
			.anyMatch(s -> s.getLabel().equals(newLabel));
		if (dup) {
			throw new DuplicateStateException(
				newLabel.getDisplay(),
				label.getDisplay(),
				id
			);
		}
	}

	private void ensureUniqueTransitionLabelForSource(Label newLabel, WorkflowState source) {
		boolean dup = transitions.stream()
			.filter(t -> !t.isArchived())
			.filter(t -> t.getSourceState().equals(source))
			.anyMatch(t -> t.getLabel().equals(newLabel));
		if (dup) {
			throw new DuplicateTransitionException(
				newLabel.getDisplay(),
				source.getDisplayLabel(),
				label.getDisplay(),
				id
			);
		}
	}
}
