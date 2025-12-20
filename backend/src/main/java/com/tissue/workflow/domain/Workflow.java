package com.tissue.workflow.domain;

import static com.tissue.workflow.domain.enums.StateCategory.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.SQLRestriction;
import org.springframework.lang.Nullable;

import com.tissue.common.entity.BaseEntity;
import com.tissue.common.enums.ColorType;
import com.tissue.common.vo.Label;
import com.tissue.project.domain.Project;
import com.tissue.workflow.domain.enums.StateCategory;
import com.tissue.workflow.domain.exception.WorkflowExceptions;
import com.tissue.workflow.domain.guard.GuardType;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Entity
@SQLRestriction("softDeleted = false")
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

	@Column(name = "project_key", nullable = false, updatable = false)
	private String projectKey;

	@Column(name = "workspace_key", nullable = false, updatable = false)
	private String workspaceKey;

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

	@OneToOne(fetch = FetchType.LAZY)
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
		wf.projectKey = project.getKey();
		wf.workspaceKey = project.getWorkspaceKey();
		wf.label = label;
		wf.description = description;
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

		WorkflowState state = WorkflowState.of(label, description, color, stateCategory);
		state.attachToWorkflow(this);
		states.add(state);

		if (stateCategory.isInitial()) {
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

	public String getDisplayName() {
		return label.getDisplay();
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

	public void setInitialState(@NonNull WorkflowState state) {
		if (!states.contains(state)) {
			throw WorkflowExceptions.initialStateBelongMismatch();
		}
		if (!state.isCategorizedAs(INITIAL)) {
			throw WorkflowExceptions.initialStateCategoryMismatch();
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
		this.description = description;
	}

	public void updateColor(@Nullable ColorType color) {
		this.color = color;
	}

	public void deleteState(@NonNull WorkflowState state) {
		if (state.getCategory().isInitial()) {
			throw WorkflowExceptions.cannotDeleteInitialState(
				this.getId(),
				this.getDisplayName(),
				state.getDisplayLabel()
			);
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
		if (state.isCategorizedAs(newCategory)) {
			return;
		}
		if (newCategory.isInitial()) {
			this.initialState = state;
		}
		if (state.getCategory().isInitial()) {
			this.initialState = null;
		}

		state.categorizeAs(newCategory);
	}

	public void rewireTransitionSource(@NonNull WorkflowTransition transition, @NonNull WorkflowState newSource) {
		transition.rewireSource(newSource);
	}

	public void rewireTransitionTarget(@NonNull WorkflowTransition transition, @NonNull WorkflowState newTarget) {
		transition.rewireTarget(newTarget);
	}

	public void addTransitionGuard(
		@NonNull WorkflowTransition transition,
		@NonNull GuardType guardType,
		@Nullable Map<String, Object> params,
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
			throw WorkflowExceptions.duplicateTransitionEdge(source.getDisplayLabel(), target.getDisplayLabel());
		}
	}

	private void ensureUniqueStateLabel(Label newLabel) {
		boolean dup = states.stream()
			.filter(t -> !t.isArchived())
			.anyMatch(s -> s.getLabel().equals(newLabel));
		if (dup) {
			throw WorkflowExceptions.duplicateStateName(
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
			throw WorkflowExceptions.duplicateTransitionName(
				newLabel.getDisplay(),
				source.getDisplayLabel(),
				label.getDisplay(),
				id
			);
		}
	}
}
