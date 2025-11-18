package com.tissue.api.workflow.domain;

import static com.tissue.api.common.util.TextNormalizer.*;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.SQLRestriction;
import org.springframework.lang.Nullable;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.common.enums.ColorType;
import com.tissue.api.common.vo.Label;
import com.tissue.api.workflow.domain.gaurd.GuardType;
import com.tissue.api.workflow.exception.DuplicateStateException;
import com.tissue.api.workflow.exception.DuplicateTransitionException;
import com.tissue.api.workspace.domain.Workspace;

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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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
public class Workflow extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@ToString.Include
	private Long id;

	@Version
	@ToString.Include
	private Long version;

	@ManyToOne(fetch = FetchType.LAZY)
	private Workspace workspace;

	@Embedded
	@ToString.Include
	private Label label;

	@Column(nullable = false, length = 255)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ColorType color;

	@OneToMany(mappedBy = "workflow", cascade = CascadeType.PERSIST, orphanRemoval = false)
	private List<WorkflowState> states = new ArrayList<>();

	@OneToMany(mappedBy = "workflow", cascade = CascadeType.PERSIST, orphanRemoval = false)
	private List<WorkflowTransition> transitions = new ArrayList<>();

	@ManyToOne(fetch = FetchType.LAZY)
	private WorkflowState initialState;

	@Column(nullable = false)
	private boolean systemProvided;

	public static Workflow create(
		@NonNull Workspace workspace,
		@NonNull Label label,
		@Nullable String description,
		@NonNull ColorType color
	) {
		Workflow wf = new Workflow();
		wf.workspace = workspace;
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
		boolean initial,
		boolean terminal
	) {
		ensureNotSystemProvided();
		ensureUniqueStateLabel(label);

		WorkflowState state = WorkflowState.of(label, description, color, initial, terminal);
		attachState(state);

		return state;
	}

	public WorkflowTransition addTransition(
		@NonNull Label label,
		@Nullable String description,
		@NonNull WorkflowState source,
		@NonNull WorkflowState target
	) {
		ensureNotSystemProvided();
		ensureUniqueTransitionLabelForSource(label, source);
		ensureNoDuplicateEdge(source, target);

		WorkflowTransition transition = WorkflowTransition.of(label, description, source, target);
		attachTransition(transition);

		return transition;
	}

	public void setAsSystemProvided() {
		this.systemProvided = true;
	}

	public void rename(@NonNull Label label) {
		ensureNotSystemProvided();
		this.label = label;
	}

	public void updateDescription(@Nullable String description) {
		this.description = nullToEmpty(description);
	}

	public void updateColor(@Nullable ColorType color) {
		this.color = color;
	}

	public void updateInitialState(@NonNull WorkflowState newInitial) {
		ensureNotSystemProvided();
		for (WorkflowState s : states) {
			s.unmarkInitial();
		}
		newInitial.markInitial();
		this.initialState = newInitial;
	}

	public List<WorkflowState> getTerminalStates() {
		return states.stream()
			.filter(WorkflowState::isTerminal)
			.toList();
	}

	// TODO: 삭제 금지 정책을 정하자
	//  전략 1: 하나 이상의 Issue가 intial status가 아니면서 Workflow를 진행 중이면 삭제 막기
	//  전략 2: 하나 이상의 IssueType이 해당 Workflow를 선택했으면 삭제 막기
	public void softDelete() {
		ensureNotSystemProvided();
		archive();
		states.forEach(WorkflowState::softDelete);
		transitions.forEach(WorkflowTransition::softDelete);
	}

	public void softDeleteState(WorkflowState state) {
		ensureNotSystemProvided();
		state.softDelete();
		states.remove(state);
	}

	public void softDeleteTransition(WorkflowTransition transition) {
		ensureNotSystemProvided();
		transition.softDelete();
		transitions.remove(transition);
	}

	public void renameState(@NonNull WorkflowState state, @NonNull Label newLabel) {
		ensureNotSystemProvided();
		if (state.getLabel().equals(newLabel)) {
			return;
		}
		ensureUniqueStateLabel(newLabel);
		state.updateLabel(newLabel);
	}

	public void renameTransition(@NonNull WorkflowTransition transition, @NonNull Label newLabel) {
		ensureNotSystemProvided();
		if (transition.getLabel().equals(newLabel)) {
			return;
		}
		ensureUniqueTransitionLabelForSource(newLabel, transition.getSourceState());
		transition.updateLabel(newLabel);
	}

	public void updateStateTerminalFlag(@NonNull WorkflowState state, boolean terminalFlag) {
		ensureNotSystemProvided();
		if (state.isTerminal() == terminalFlag) {
			return;
		}
		if (terminalFlag) {
			state.markTerminal();
			return;
		}
		state.unmarkTerminal();
	}

	public void rewireTransitionSource(@NonNull WorkflowTransition transition, @NonNull WorkflowState newSource) {
		ensureNotSystemProvided();
		transition.rewireSource(newSource);
	}

	public void rewireTransitionTarget(@NonNull WorkflowTransition transition, @NonNull WorkflowState newTarget) {
		ensureNotSystemProvided();
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

	private void attachState(WorkflowState state) {
		state.attachToWorkflow(this);
		states.add(state);

		if (state.isInitial()) {
			updateInitialState(state);
		}
	}

	private void attachTransition(WorkflowTransition transition) {
		transition.attachToWorkflow(this);
		transitions.add(transition);
	}

	private void ensureNotSystemProvided() {
		if (systemProvided) {
			throw new RuntimeException("Cannot modify system provided workflow.");
		}
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
