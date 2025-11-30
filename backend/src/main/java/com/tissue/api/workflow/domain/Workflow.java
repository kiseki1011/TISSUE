package com.tissue.api.workflow.domain;

import static com.tissue.api.common.util.TextNormalizer.*;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.SQLRestriction;
import org.springframework.lang.Nullable;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.common.enums.ColorType;
import com.tissue.api.common.vo.Label;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

// TODO: softDeleted = false인 경우에만 적용하는 unique constraint 필요 -> Postgres DDL 사용
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

	@Embedded
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
	//  전략 1: 하나 이상의 Issue가 intial status가 아니면서 Workflow를 진행했으면 삭제 불가
	//  전략 2: 하나 이상의 IssueType이 해당 Workflow를 선택했으면 삭제 불가
	//  전략 3: 전략 1 + 전략 2 둘다 사용

	// TODO: soft delete vs hard delete
	//  어떤게 좋을까? Workspace, Project, Issue 등과 같은 리소스는 soft-delete이 이해가지만,
	//  Workflow도 soft-delete 정책을 사용하는게 좋을까?
	public void delete() {
		ensureNotSystemProvided();
		softDelete();
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
